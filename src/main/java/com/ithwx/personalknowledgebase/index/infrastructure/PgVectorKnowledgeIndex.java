package com.ithwx.personalknowledgebase.index.infrastructure;

import com.ithwx.personalknowledgebase.index.domain.KnowledgeChunk;
import com.ithwx.personalknowledgebase.index.domain.KnowledgeIndex;
import com.ithwx.personalknowledgebase.index.domain.SearchQuery;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PgVectorKnowledgeIndex implements KnowledgeIndex {

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
    public List<KnowledgeChunk> search(SearchQuery query) {
        return merge(vectorSearch(query), keywordSearch(query));
    }

    @Override
    public void replace(Long documentId, List<KnowledgeChunk> chunks) {
        delete(documentId);
        if (!chunks.isEmpty()) {
            vectorStore.add(toVectorDocuments(chunks));
        }
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
                       (metadata ->> 'chunkIndex')::integer AS chunk_index
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
                resultSet.getString("content")
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
                document.getText()
        );
    }

    List<KnowledgeChunk> merge(
            List<KnowledgeChunk> vectorResults,
            List<KnowledgeChunk> keywordResults
    ) {
        Map<String, KnowledgeChunk> uniqueChunks = new LinkedHashMap<>();
        addUnique(vectorResults, uniqueChunks);
        addUnique(keywordResults, uniqueChunks);
        return List.copyOf(uniqueChunks.values());
    }

    private void addUnique(
            List<KnowledgeChunk> candidates,
            Map<String, KnowledgeChunk> uniqueChunks
    ) {
        for (KnowledgeChunk chunk : candidates) {
            String key = chunk.documentId() + ":" + chunk.chunkIndex();
            uniqueChunks.putIfAbsent(key, chunk);
        }
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

}
