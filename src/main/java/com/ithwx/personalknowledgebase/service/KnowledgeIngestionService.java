package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.library.domain.Document;
import com.ithwx.personalknowledgebase.library.domain.ContentHash;
import com.ithwx.personalknowledgebase.library.domain.DocumentDeleted;
import com.ithwx.personalknowledgebase.library.domain.DocumentStatus;
import com.ithwx.personalknowledgebase.library.domain.DocumentRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class KnowledgeIngestionService {

    private final ChunkingService chunkingService;
    private final DocumentRepository documentRepository;
    private final VectorStore vectorStore;

    public KnowledgeIngestionService(
            ChunkingService chunkingService,
            DocumentRepository documentRepository,
            VectorStore vectorStore
    ) {
        this.chunkingService = chunkingService;
        this.documentRepository = documentRepository;
        this.vectorStore = vectorStore;
    }

    public Document ingest(
            String name,
            String sourceType,
            String sourceUrl,
            String content
    ) {
        validate(name, sourceType, content);

        String contentHash = ContentHash.of(content).value();
        if (documentRepository.existsByContentHash(contentHash)) {
            throw new IllegalArgumentException("相同内容的资料已存在");
        }

        List<String> chunks = chunkingService.chunk(content);

        Document entity = new Document();
        entity.setName(name.strip());
        entity.setFileType(sourceType.strip().toLowerCase(Locale.ROOT));
        entity.setSourceUrl(sourceUrl == null || sourceUrl.isBlank() ? null : sourceUrl.strip());
        entity.setContent(content);
        entity.setContentHash(contentHash);
        entity.setStatus(DocumentStatus.PROCESSING.name());
        entity = documentRepository.save(entity);

        return storeVectors(entity, chunks);
    }

    public Document replace(
            Document entity,
            String name,
            String sourceType,
            String sourceUrl,
            String content
    ) {
        validate(name, sourceType, content);

        String contentHash = ContentHash.of(content).value();
        if (documentRepository.existsOtherWithHash(contentHash, entity.getId())) {
            throw new IllegalArgumentException("相同内容的资料已存在");
        }

        List<String> chunks = chunkingService.chunk(content);

        entity.setName(name.strip());
        entity.setFileType(sourceType.strip().toLowerCase(Locale.ROOT));
        entity.setSourceUrl(sourceUrl == null || sourceUrl.isBlank() ? null : sourceUrl.strip());
        entity.setContent(content);
        entity.setContentHash(contentHash);
        entity.setStatus(DocumentStatus.PROCESSING.name());
        entity.setChunkCount(0);
        documentRepository.save(entity);
        vectorStore.delete(documentFilter(entity.getId()));

        return storeVectors(entity, chunks);
    }

    @EventListener
    public void onDocumentDeleted(DocumentDeleted event) {
        deleteVectors(event.documentId());
    }

    public void deleteVectors(Long documentId) {
        vectorStore.delete(documentFilter(documentId));
    }

    private Document storeVectors(Document entity, List<String> chunks) {
        try {
            vectorStore.add(toVectorDocuments(entity, chunks));
            entity.setStatus(DocumentStatus.READY.name());
            entity.setChunkCount(chunks.size());
            return documentRepository.save(entity);
        } catch (RuntimeException exception) {
            entity.setStatus(DocumentStatus.FAILED.name());
            documentRepository.save(entity);
            throw exception;
        }
    }

    private Filter.Expression documentFilter(Long documentId) {
        return new FilterExpressionBuilder()
                .eq("documentId", String.valueOf(documentId))
                .build();
    }

    private void validate(String name, String sourceType, String content) {
        if (name == null || name.isBlank()
                || sourceType == null || sourceType.isBlank()
                || content == null || content.isBlank()) {
            throw new IllegalArgumentException("资料名称、类型和正文不能为空");
        }
    }

    private List<org.springframework.ai.document.Document> toVectorDocuments(Document entity, List<String> chunks) {
        List<org.springframework.ai.document.Document> documents = new ArrayList<>();

        for (int index = 0; index < chunks.size(); index++) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("documentId", String.valueOf(entity.getId()));
            metadata.put("documentName", entity.getName());
            metadata.put("sourceType", entity.getFileType());
            metadata.put("chunkIndex", index);
            if (entity.getSourceUrl() != null) {
                metadata.put("sourceUrl", entity.getSourceUrl());
            }

            documents.add(org.springframework.ai.document.Document.builder()
                    .text(chunks.get(index))
                    .metadata(metadata)
                    .build());
        }

        return documents;
    }
}
