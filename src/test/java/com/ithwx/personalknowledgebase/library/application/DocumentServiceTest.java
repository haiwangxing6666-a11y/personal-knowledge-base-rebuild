package com.ithwx.personalknowledgebase.library.application;

import com.ithwx.personalknowledgebase.library.domain.Document;
import com.ithwx.personalknowledgebase.library.domain.DocumentDeleted;
import com.ithwx.personalknowledgebase.library.domain.DocumentMetadataUpdated;
import com.ithwx.personalknowledgebase.library.domain.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository repository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void shouldListAndFindDocuments() {
        Document document = document(1L);
        DocumentService service = new DocumentService(repository, eventPublisher);
        when(repository.listNewestFirst()).thenReturn(List.of(document));
        when(repository.findById(1L)).thenReturn(Optional.of(document));

        assertEquals(List.of(document), service.list());
        assertSame(document, service.get(1L));
    }

    @Test
    void shouldRejectUnknownDocument() {
        DocumentService service = new DocumentService(repository, eventPublisher);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.get(99L));
    }

    @Test
    void shouldSaveMetadataBeforeNotifyingIndex() {
        Document document = document(1L);
        DocumentService service = new DocumentService(repository, eventPublisher);
        when(repository.findById(1L)).thenReturn(Optional.of(document));
        when(repository.save(document)).thenReturn(document);

        assertSame(document, service.updateMetadata(1L, " Java ", Set.of(" 数据库 ")));
        assertEquals("Java", document.getCategory());
        assertEquals(Set.of("数据库"), document.getTags());
        InOrder order = inOrder(repository, eventPublisher);
        order.verify(repository).save(document);
        order.verify(eventPublisher).publishEvent(new DocumentMetadataUpdated(1L));
    }

    @Test
    void shouldDeleteBeforeNotifyingIndex() {
        Document document = document(1L);
        DocumentService service = new DocumentService(repository, eventPublisher);
        when(repository.findById(1L)).thenReturn(Optional.of(document));

        service.delete(1L);

        InOrder order = inOrder(repository, eventPublisher);
        order.verify(repository).delete(document);
        order.verify(eventPublisher).publishEvent(new DocumentDeleted(1L));
    }

    private Document document(Long id) {
        Document document = new Document();
        document.setId(id);
        return document;
    }
}
