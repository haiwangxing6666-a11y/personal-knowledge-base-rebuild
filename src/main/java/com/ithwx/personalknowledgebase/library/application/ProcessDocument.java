package com.ithwx.personalknowledgebase.library.application;

import com.ithwx.personalknowledgebase.library.domain.ContentHash;
import com.ithwx.personalknowledgebase.library.domain.Document;
import com.ithwx.personalknowledgebase.library.domain.DocumentRepository;
import com.ithwx.personalknowledgebase.library.domain.DocumentStatus;
import com.ithwx.personalknowledgebase.library.domain.DocumentTextReady;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.NoSuchElementException;

@Component
class ProcessDocument {

    private static final int MAX_FAILURE_REASON_LENGTH = 1000;

    private final DocumentRepository repository;
    private final DocumentExtractor extractor;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskExecutor taskExecutor;

    ProcessDocument(
            DocumentRepository repository,
            DocumentExtractor extractor,
            ApplicationEventPublisher eventPublisher,
            TaskExecutor taskExecutor
    ) {
        this.repository = repository;
        this.extractor = extractor;
        this.eventPublisher = eventPublisher;
        this.taskExecutor = taskExecutor;
    }

    public void processAsync(Long documentId) {
        taskExecutor.execute(() -> process(documentId));
    }

    void process(Long documentId) {
        Document document = requiredDocument(documentId);
        document.setStatus(DocumentStatus.PROCESSING.name());
        document.setFailureReason(null);
        document = repository.save(document);

        try {
            DocumentExtractor.Result extracted = extractor.extract(document);
            String hash = ContentHash.of(extracted.content()).value();
            if (repository.existsOtherWithHash(hash, documentId)) {
                throw new IllegalArgumentException("相同内容的资料已存在");
            }
            document.setName(extracted.name());
            document.setSourceUrl(extracted.sourceUrl());
            document.setContent(extracted.content());
            document.setContentHash(hash);
            document = repository.save(document);
            eventPublisher.publishEvent(new DocumentTextReady(
                    document.getId(),
                    document.getName(),
                    document.getFileType(),
                    document.getSourceUrl(),
                    document.getContent()
            ));
        } catch (Exception exception) {
            document.setContentHash(null);
            fail(document, messageOf(exception));
        }
    }

    Document retry(Long id) {
        Document document = requiredDocument(id);
        if (!DocumentStatus.FAILED.name().equals(document.getStatus())) {
            throw new IllegalArgumentException("只有处理失败的资料可以重试");
        }
        document.setStatus(DocumentStatus.PENDING.name());
        document.setFailureReason(null);
        Document saved = repository.save(document);
        processAsync(saved.getId());
        return saved;
    }

    void markReady(Long id, int chunkCount) {
        Document document = requiredDocument(id);
        document.setStatus(DocumentStatus.READY.name());
        document.setChunkCount(chunkCount);
        document.setFailureReason(null);
        repository.save(document);
    }

    void markFailed(Long id, String reason) {
        fail(requiredDocument(id), reason);
    }

    @EventListener(ApplicationReadyEvent.class)
    void resumeUnfinishedDocuments() {
        List<String> statuses = List.of(
                DocumentStatus.PENDING.name(),
                DocumentStatus.PROCESSING.name()
        );
        for (Document document : repository.findByStatuses(statuses)) {
            processAsync(document.getId());
        }
    }

    private Document requiredDocument(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("资料不存在：" + id));
    }

    private void fail(Document document, String reason) {
        document.setStatus(DocumentStatus.FAILED.name());
        document.setFailureReason(shortReason(reason));
        repository.save(document);
    }

    private String messageOf(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName() : message;
    }

    private String shortReason(String reason) {
        String value = reason == null || reason.isBlank() ? "处理失败" : reason;
        return value.length() <= MAX_FAILURE_REASON_LENGTH
                ? value : value.substring(0, MAX_FAILURE_REASON_LENGTH);
    }
}
