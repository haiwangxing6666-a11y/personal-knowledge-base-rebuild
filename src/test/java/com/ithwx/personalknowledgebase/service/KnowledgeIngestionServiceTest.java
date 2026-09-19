package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.library.application.DocumentService;
import com.ithwx.personalknowledgebase.library.domain.DocumentDeleted;
import com.ithwx.personalknowledgebase.library.domain.DocumentTextReady;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeIngestionServiceTest {

    @Mock
    private ChunkingService chunkingService;
    @Mock
    private VectorStore vectorStore;
    @Mock
    private DocumentService documentService;

    private KnowledgeIngestionService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeIngestionService(
                chunkingService, vectorStore, documentService);
    }

    @Test
    void shouldSplitStoreAndMarkDocumentReady() {
        when(chunkingService.chunk("第一段\n\n第二段"))
                .thenReturn(List.of("第一段", "第二段"));

        service.onDocumentTextReady(new DocumentTextReady(
                1L, "Spring 笔记", "note", null, "第一段\n\n第二段"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<org.springframework.ai.document.Document>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        assertEquals("第一段", captor.getValue().get(0).getText());
        verify(documentService).markReady(1L, 2);
    }

    @Test
    void shouldMarkDocumentFailedWhenVectorStoreFails() {
        when(chunkingService.chunk("正文")).thenReturn(List.of("正文"));
        doThrow(new RuntimeException("向量服务失败")).when(vectorStore).add(anyList());

        service.onDocumentTextReady(new DocumentTextReady(
                1L, "失败资料", "txt", null, "正文"));

        verify(documentService).markFailed(1L, "向量服务失败");
    }

    @Test
    void shouldDeleteVectorsWhenDocumentIsDeleted() {
        service.onDocumentDeleted(new DocumentDeleted(1L));

        verify(vectorStore).delete(
                org.mockito.ArgumentMatchers.any(Filter.Expression.class));
    }
}
