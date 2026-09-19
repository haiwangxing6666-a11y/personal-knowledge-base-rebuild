package com.ithwx.personalknowledgebase.library.application;

import com.ithwx.personalknowledgebase.library.domain.Document;
import com.ithwx.personalknowledgebase.library.domain.DocumentRepository;
import com.ithwx.personalknowledgebase.library.domain.DocumentTextReady;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessDocumentTest {

    @Mock
    private DocumentRepository repository;
    @Mock
    private DocumentExtractor extractor;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private TaskExecutor taskExecutor;

    private ProcessDocument processDocument;
    private Document document;

    @BeforeEach
    void setUp() {
        processDocument = new ProcessDocument(
                repository, extractor, eventPublisher, taskExecutor);
        document = new Document();
        document.setId(1L);
        document.setName("笔记.txt");
        document.setFileType("txt");
        document.setCategory("Java");
        document.setTags(java.util.Set.of("基础"));
        document.setStatus("PENDING");
    }

    @Test
    void shouldExtractTextCheckDuplicateAndNotifyIndex() throws Exception {
        stubDocument();
        when(extractor.extract(document))
                .thenReturn(new DocumentExtractor.Result("笔记.txt", "正文", null));

        processDocument.process(1L);

        assertEquals("PROCESSING", document.getStatus());
        assertEquals("正文", document.getContent());
        assertNotNull(document.getContentHash());
        ArgumentCaptor<DocumentTextReady> event = ArgumentCaptor.forClass(DocumentTextReady.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals("正文", event.getValue().content());
        assertEquals("Java", event.getValue().category());
        assertEquals(java.util.Set.of("基础"), event.getValue().tags());
    }

    @Test
    void shouldFailWithoutNotifyingIndexWhenContentIsDuplicate() throws Exception {
        stubDocument();
        when(extractor.extract(document))
                .thenReturn(new DocumentExtractor.Result("笔记.txt", "重复正文", null));
        when(repository.existsOtherWithHash(anyString(), org.mockito.ArgumentMatchers.eq(1L)))
                .thenReturn(true);

        processDocument.process(1L);

        assertEquals("FAILED", document.getStatus());
        assertEquals("相同内容的资料已存在", document.getFailureReason());
        verify(eventPublisher, never()).publishEvent(any(DocumentTextReady.class));
    }

    @Test
    void shouldRetryAndReceiveIndexResult() {
        stubDocument();
        document.setStatus("FAILED");
        processDocument.retry(1L);
        verify(repository).save(document);
        verify(taskExecutor).execute(any(Runnable.class));

        processDocument.markReady(1L, 3);
        assertEquals("READY", document.getStatus());
        assertEquals(3, document.getChunkCount());

        processDocument.markFailed(1L, "向量服务失败");
        assertEquals("FAILED", document.getStatus());
        assertEquals("向量服务失败", document.getFailureReason());
    }

    @Test
    void shouldResumeUnfinishedDocumentsAfterRestart() {
        Document another = new Document();
        another.setId(2L);
        when(repository.findByStatuses(List.of("PENDING", "PROCESSING")))
                .thenReturn(List.of(document, another));

        processDocument.resumeUnfinishedDocuments();

        verify(repository).findByStatuses(List.of("PENDING", "PROCESSING"));
        verify(taskExecutor, org.mockito.Mockito.times(2)).execute(any(Runnable.class));
    }

    private void stubDocument() {
        when(repository.findById(1L)).thenReturn(Optional.of(document));
        when(repository.save(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
