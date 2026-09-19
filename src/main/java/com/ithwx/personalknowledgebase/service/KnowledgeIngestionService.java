package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.library.application.DocumentService;
import com.ithwx.personalknowledgebase.library.domain.DocumentDeleted;
import com.ithwx.personalknowledgebase.library.domain.DocumentTextReady;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeIngestionService {

    private final ChunkingService chunkingService;
    private final VectorStore vectorStore;
    private final DocumentService documentService;

    public KnowledgeIngestionService(
            ChunkingService chunkingService,
            VectorStore vectorStore,
            DocumentService documentService
    ) {
        this.chunkingService = chunkingService;
        this.vectorStore = vectorStore;
        this.documentService = documentService;
    }

    @EventListener
    public void onDocumentTextReady(DocumentTextReady event) {
        try {
            List<String> chunks = chunkingService.chunk(event.content());
            vectorStore.delete(documentFilter(event.documentId()));
            vectorStore.add(toVectorDocuments(event, chunks));
            documentService.markReady(event.documentId(), chunks.size());
        } catch (RuntimeException exception) {
            String reason = exception.getMessage() == null
                    ? "向量索引建立失败" : exception.getMessage();
            documentService.markFailed(event.documentId(), reason);
        }
    }

    @EventListener
    public void onDocumentDeleted(DocumentDeleted event) {
        vectorStore.delete(documentFilter(event.documentId()));
    }

    private Filter.Expression documentFilter(Long documentId) {
        return new FilterExpressionBuilder()
                .eq("documentId", String.valueOf(documentId))
                .build();
    }

    private List<org.springframework.ai.document.Document> toVectorDocuments(
            DocumentTextReady event,
            List<String> chunks
    ) {
        List<org.springframework.ai.document.Document> documents = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("documentId", String.valueOf(event.documentId()));
            metadata.put("documentName", event.name());
            metadata.put("sourceType", event.sourceType());
            metadata.put("chunkIndex", index);
            if (event.sourceUrl() != null) {
                metadata.put("sourceUrl", event.sourceUrl());
            }
            documents.add(org.springframework.ai.document.Document.builder()
                    .text(chunks.get(index))
                    .metadata(metadata)
                    .build());
        }
        return documents;
    }
}
