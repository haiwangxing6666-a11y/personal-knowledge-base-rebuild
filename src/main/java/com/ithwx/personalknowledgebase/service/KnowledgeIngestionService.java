package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.entity.DocumentEntity;
import com.ithwx.personalknowledgebase.repository.DocumentRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
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

    public DocumentEntity ingest(
            String name,
            String sourceType,
            String sourceUrl,
            String content
    ) {
        validate(name, sourceType, content);

        String contentHash = sha256(content);
        if (documentRepository.existsByContentHash(contentHash)) {
            throw new IllegalArgumentException("相同内容的资料已存在");
        }

        List<String> chunks = chunkingService.chunk(content);

        DocumentEntity entity = new DocumentEntity();
        entity.setName(name.strip());
        entity.setFileType(sourceType.strip().toLowerCase(Locale.ROOT));
        entity.setSourceUrl(sourceUrl == null || sourceUrl.isBlank() ? null : sourceUrl.strip());
        entity.setContent(content);
        entity.setContentHash(contentHash);
        entity.setStatus("PROCESSING");
        entity = documentRepository.save(entity);

        return storeVectors(entity, chunks);
    }

    public DocumentEntity replace(
            DocumentEntity entity,
            String name,
            String sourceType,
            String sourceUrl,
            String content
    ) {
        validate(name, sourceType, content);

        String contentHash = sha256(content);
        if (documentRepository.existsByContentHashAndIdNot(contentHash, entity.getId())) {
            throw new IllegalArgumentException("相同内容的资料已存在");
        }

        List<String> chunks = chunkingService.chunk(content);
        vectorStore.delete(documentFilter(entity.getId()));

        entity.setName(name.strip());
        entity.setFileType(sourceType.strip().toLowerCase(Locale.ROOT));
        entity.setSourceUrl(sourceUrl == null || sourceUrl.isBlank() ? null : sourceUrl.strip());
        entity.setContent(content);
        entity.setContentHash(contentHash);
        entity.setStatus("PROCESSING");
        entity.setChunkCount(0);
        documentRepository.save(entity);

        return storeVectors(entity, chunks);
    }

    public void deleteVectors(Long documentId) {
        vectorStore.delete(documentFilter(documentId));
    }

    private DocumentEntity storeVectors(DocumentEntity entity, List<String> chunks) {
        try {
            vectorStore.add(toVectorDocuments(entity, chunks));
            entity.setStatus("READY");
            entity.setChunkCount(chunks.size());
            return documentRepository.save(entity);
        } catch (RuntimeException exception) {
            entity.setStatus("FAILED");
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

    private List<Document> toVectorDocuments(DocumentEntity entity, List<String> chunks) {
        List<Document> documents = new ArrayList<>();

        for (int index = 0; index < chunks.size(); index++) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("documentId", String.valueOf(entity.getId()));
            metadata.put("documentName", entity.getName());
            metadata.put("sourceType", entity.getFileType());
            metadata.put("chunkIndex", index);
            if (entity.getSourceUrl() != null) {
                metadata.put("sourceUrl", entity.getSourceUrl());
            }

            documents.add(Document.builder()
                    .text(chunks.get(index))
                    .metadata(metadata)
                    .build());
        }

        return documents;
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(content.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256", exception);
        }
    }
}
