package com.ithwx.personalknowledgebase.index.application;

import com.ithwx.personalknowledgebase.index.domain.KnowledgeChunk;
import com.ithwx.personalknowledgebase.index.domain.KnowledgeIndex;
import com.ithwx.personalknowledgebase.index.infrastructure.TextChunker;
import com.ithwx.personalknowledgebase.library.application.DocumentService;
import com.ithwx.personalknowledgebase.library.domain.DocumentDeleted;
import com.ithwx.personalknowledgebase.library.domain.DocumentTextReady;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexDocumentTest {

    @Mock
    private TextChunker textChunker;
    @Mock
    private KnowledgeIndex knowledgeIndex;
    @Mock
    private DocumentService documentService;

    private IndexDocument indexDocument;

    @BeforeEach
    void setUp() {
        indexDocument = new IndexDocument(textChunker, knowledgeIndex, documentService);
    }

    @Test
    void shouldBuildIndexAndMarkDocumentReady() {
        DocumentTextReady event = event();
        when(textChunker.split(event.content())).thenReturn(List.of("第一段", "第二段"));

        indexDocument.onTextReady(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<KnowledgeChunk>> chunks = ArgumentCaptor.forClass(List.class);
        verify(knowledgeIndex).replace(org.mockito.ArgumentMatchers.eq(1L), chunks.capture());
        assertEquals("第一段", chunks.getValue().get(0).text());
        assertEquals("Spring 笔记", chunks.getValue().get(0).documentName());
        verify(documentService).markReady(1L, 2);
    }

    @Test
    void shouldMarkDocumentFailedWhenIndexFails() {
        DocumentTextReady event = event();
        when(textChunker.split(event.content())).thenReturn(List.of("正文"));
        doThrow(new RuntimeException("向量服务失败"))
                .when(knowledgeIndex).replace(org.mockito.ArgumentMatchers.eq(1L), anyList());

        indexDocument.onTextReady(event);

        verify(documentService).markFailed(1L, "向量服务失败");
    }

    @Test
    void shouldSynchronizeDeletion() {
        indexDocument.onDocumentDeleted(new DocumentDeleted(1L));

        verify(knowledgeIndex).delete(1L);
    }

    private DocumentTextReady event() {
        return new DocumentTextReady(
                1L,
                "Spring 笔记",
                "note",
                null,
                "第一段\n\n第二段"
        );
    }
}
