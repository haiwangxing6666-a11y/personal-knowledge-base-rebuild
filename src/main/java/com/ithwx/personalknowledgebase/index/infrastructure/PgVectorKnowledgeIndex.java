package com.ithwx.personalknowledgebase.index.infrastructure;

import com.ithwx.personalknowledgebase.index.domain.KnowledgeChunk;
import com.ithwx.personalknowledgebase.index.domain.KnowledgeIndex;
import com.ithwx.personalknowledgebase.index.domain.SearchQuery;
import com.ithwx.personalknowledgebase.index.domain.SearchResult;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class PgVectorKnowledgeIndex implements KnowledgeIndex {

    private static final int RRF_K = 60;

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public PgVectorKnowledgeIndex(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeKeywordIndex() {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_vector_store_content_trgm
                ON vector_store USING gin (content gin_trgm_ops)
                """);
    }

    @Override
    public List<SearchResult> search(SearchQuery query) {
        List<KnowledgeChunk> vectorResults = vectorSearch(query).stream()
                .filter(chunk -> matchesFilters(chunk, query))
                .toList();
        List<KnowledgeChunk> keywordResults = keywordSearch(query).stream()
                .filter(chunk -> matchesFilters(chunk, query))
                .toList();
        return rerank(vectorResults, keywordResults);
    }

    @Override
    public void replace(Long documentId, List<KnowledgeChunk> chunks) {
        delete(documentId);
        if (!chunks.isEmpty()) {
            vectorStore.add(toVectorDocuments(chunks));
        }
    }

    @Override
    public void updateMetadata(Long documentId, String category, Set<String> tags) {
        jdbcTemplate.update("""
                UPDATE vector_store
                SET metadata = (metadata::jsonb
                    || jsonb_build_object('category', ?, 'tags', ?))::json
                WHERE metadata ->> 'documentId' = ?
                """, normalizedCategory(category), tagsText(tags), String.valueOf(documentId));
    }

    @Override
    public void delete(Long documentId) {
        vectorStore.delete(documentFilter(documentId));
    }

    private List<KnowledgeChunk> vectorSearch(SearchQuery query) {
        SearchRequest request = SearchRequest.builder()
                .query(query.text())
                .topK(query.candidateLimit())
                .similarityThreshold(query.similarityThreshold())
                .build();

        return vectorStore.similaritySearch(request).stream()
                .map(this::toKnowledgeChunk)
                .toList();
    }

    private List<KnowledgeChunk> keywordSearch(SearchQuery query) {
        return jdbcTemplate.query("""
                SELECT content,
                       metadata ->> 'documentId' AS document_id,
                       metadata ->> 'documentName' AS document_name,
                       metadata ->> 'sourceType' AS source_type,
                       metadata ->> 'sourceUrl' AS source_url,
                       (metadata ->> 'chunkIndex')::integer AS chunk_index,
                       metadata ->> 'category' AS category,
                       metadata ->> 'tags' AS tags
                FROM vector_store
                WHERE content % ? OR content ILIKE '%' || ? || '%'
                ORDER BY similarity(content, ?) DESC
                LIMIT ?
                """, (resultSet, rowNumber) -> new KnowledgeChunk(
                resultSet.getLong("document_id"),
                resultSet.getString("document_name"),
                resultSet.getString("source_type"),
                resultSet.getString("source_url"),
                resultSet.getInt("chunk_index"),
                resultSet.getString("content"),
                resultSet.getString("category"),
                parseTags(resultSet.getString("tags"))
        ), query.text(), query.text(), query.text(), query.candidateLimit());
    }

    private KnowledgeChunk toKnowledgeChunk(org.springframework.ai.document.Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new KnowledgeChunk(
                Long.valueOf(metadata.get("documentId").toString()),
                metadata.get("documentName").toString(),
                metadata.get("sourceType").toString(),
                metadata.get("sourceUrl") == null ? null : metadata.get("sourceUrl").toString(),
                Integer.parseInt(metadata.get("chunkIndex").toString()),
                document.getText(),
                metadata.getOrDefault("category", "").toString(),
                parseTags(metadata.getOrDefault("tags", "").toString())
        );
    }

    boolean matchesFilters(KnowledgeChunk chunk, SearchQuery query) {
        boolean categoryMatches = query.category() == null
                || query.category().isBlank()
                || query.category().equals(chunk.category());
        return categoryMatches && chunk.tags().containsAll(query.tags());
    }

    List<SearchResult> rerank(
            List<KnowledgeChunk> vectorResults,
            List<KnowledgeChunk> keywordResults
    ) {
        Map<String, KnowledgeChunk> chunks = new LinkedHashMap<>();
        Map<String, Double> scores = new LinkedHashMap<>();
        addRanking(vectorResults, chunks, scores);
        addRanking(keywordResults, chunks, scores);

        double maxScore = scores.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(1.0);

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .map(entry -> new SearchResult(
                        chunks.get(entry.getKey()),
                        entry.getValue() / maxScore
                ))
                .toList();
    }

    private void addRanking(
            List<KnowledgeChunk> ranking,
            Map<String, KnowledgeChunk> chunks,
            Map<String, Double> scores
    ) {
        for (int index = 0; index < ranking.size(); index++) {
            KnowledgeChunk chunk = ranking.get(index);
            String key = chunk.documentId() + ":" + chunk.chunkIndex();
            chunks.putIfAbsent(key, chunk);
            scores.merge(key, 1.0 / (RRF_K + index + 1), Double::sum);
        }
    }

    private Set<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(tags.split(","))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Filter.Expression documentFilter(Long documentId) {
        return new FilterExpressionBuilder()
                .eq("documentId", String.valueOf(documentId))
                .build();
    }

    private List<org.springframework.ai.document.Document> toVectorDocuments(
            List<KnowledgeChunk> chunks
    ) {
        List<org.springframework.ai.document.Document> documents = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("documentId", String.valueOf(chunk.documentId()));
            metadata.put("documentName", chunk.documentName());
            metadata.put("sourceType", chunk.sourceType());
            metadata.put("chunkIndex", chunk.chunkIndex());
            metadata.put("category", normalizedCategory(chunk.category()));
            metadata.put("tags", tagsText(chunk.tags()));
            if (chunk.sourceUrl() != null) {
                metadata.put("sourceUrl", chunk.sourceUrl());
            }
            documents.add(org.springframework.ai.document.Document.builder()
                    .text(chunk.text())
                    .metadata(metadata)
                    .build());
        }
        return documents;
    }

    private String normalizedCategory(String category) {
        return category == null ? "" : category;
    }

    private String tagsText(Set<String> tags) {
        if (tags == null) {
            return "";
        }
        return tags.stream().sorted().collect(Collectors.joining(","));
    }
}
