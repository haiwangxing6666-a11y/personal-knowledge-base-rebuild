package com.ithwx.personalknowledgebase.library.application;

import com.ithwx.personalknowledgebase.library.domain.Document;
import com.ithwx.personalknowledgebase.library.domain.DocumentDeleted;
import com.ithwx.personalknowledgebase.library.domain.DocumentRepository;
import com.ithwx.personalknowledgebase.library.domain.DocumentStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;

@Component
class ManageDocument {

    private static final Set<String> SUPPORTED_FILE_TYPES =
            Set.of("txt", "md", "markdown", "pdf", "docx");

    private final DocumentRepository repository;
    private final FileStorage fileStorage;
    private final ProcessDocument processDocument;
    private final ApplicationEventPublisher eventPublisher;

    ManageDocument(
            DocumentRepository repository,
            FileStorage fileStorage,
            ProcessDocument processDocument,
            ApplicationEventPublisher eventPublisher
    ) {
        this.repository = repository;
        this.fileStorage = fileStorage;
        this.processDocument = processDocument;
        this.eventPublisher = eventPublisher;
    }

    List<Document> list() {
        return repository.listNewestFirst();
    }

    Document get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("资料不存在：" + id));
    }

    Document update(Long id, String name, String content) {
        Document document = get(id);
        document.setName(name.strip());
        document.setContent(content);
        resetProcessingState(document);
        return saveAndProcess(document);
    }

    Document replaceFile(Long id, String filename, byte[] content) throws IOException {
        if (content.length == 0) {
            throw new IllegalArgumentException("文件内容不能为空");
        }
        Document document = get(id);
        document.setName(filename);
        document.setFileType(fileType(filename));
        document.setFilePath(fileStorage.save(filename, content));
        document.setSourceUrl(null);
        document.setContent(null);
        resetProcessingState(document);
        return saveAndProcess(document);
    }

    void delete(Long id) {
        Document document = get(id);
        repository.delete(document);
        eventPublisher.publishEvent(new DocumentDeleted(id));
    }

    private Document saveAndProcess(Document document) {
        Document saved = repository.save(document);
        processDocument.processAsync(saved.getId());
        return saved;
    }

    private void resetProcessingState(Document document) {
        document.setContentHash(null);
        document.setStatus(DocumentStatus.PENDING.name());
        document.setChunkCount(0);
        document.setFailureReason(null);
    }

    private String fileType(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw new IllegalArgumentException("无法识别文件格式");
        }
        String type = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_FILE_TYPES.contains(type)) {
            throw new IllegalArgumentException("不支持的文件格式：" + type);
        }
        return type;
    }
}
