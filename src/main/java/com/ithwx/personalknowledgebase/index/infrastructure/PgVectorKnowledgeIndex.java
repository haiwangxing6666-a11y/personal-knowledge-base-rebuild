package com.ithwx.personalknowledgebase.index.infrastructure;

import com.ithwx.personalknowledgebase.index.domain.KnowledgeChunk;
import com.ithwx.personalknowledgebase.index.domain.KnowledgeIndex;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class PgVectorKnowledgeIndex implements KnowledgeIndex {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public PgVectorKnowledgeIndex(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
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
