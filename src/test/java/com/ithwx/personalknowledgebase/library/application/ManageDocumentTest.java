package com.ithwx.personalknowledgebase.library.application;

import com.ithwx.personalknowledgebase.library.domain.Document;
import com.ithwx.personalknowledgebase.library.domain.DocumentDeleted;
import com.ithwx.personalknowledgebase.library.domain.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageDocumentTest {

    @Mock
    private DocumentRepository repository;
    @Mock
    private FileStorage fileStorage;
    @Mock
    private ProcessDocument processDocument;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ManageDocument manageDocument;
    private Document document;

    @BeforeEach
    void setUp() {
        manageDocument = new ManageDocument(
                repository, fileStorage, processDocument, eventPublisher);
        document = new Document();
        document.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(document));
    }

    @Test
    void shouldListAndGet() {
        when(repository.listNewestFirst()).thenReturn(List.of(document));

        assertSame(document, manageDocument.list().get(0));
        assertSame(document, manageDocument.get(1L));
    }

    @Test
    void shouldUpdateContentAndStartProcessing() {
        when(repository.save(document)).thenReturn(document);

        manageDocument.update(1L, "新名称", "新正文");

        assertEquals("PENDING", document.getStatus());
        assertEquals("新正文", document.getContent());
        verify(processDocument).processAsync(1L);
    }

    @Test
    void shouldDeleteBeforePublishingEvent() {
        manageDocument.delete(1L);

        InOrder order = inOrder(repository, eventPublisher);
        order.verify(repository).delete(document);
        order.verify(eventPublisher).publishEvent(new DocumentDeleted(1L));
    }
}
