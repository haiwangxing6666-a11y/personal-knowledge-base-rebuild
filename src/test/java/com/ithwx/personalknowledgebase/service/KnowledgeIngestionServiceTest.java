package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.entity.DocumentEntity;
import com.ithwx.personalknowledgebase.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeIngestionServiceTest {

    @Mock
    private ChunkingService chunkingService;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private VectorStore vectorStore;

    private KnowledgeIngestionService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeIngestionService(chunkingService, documentRepository, vectorStore);
        when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(invocation -> {
            DocumentEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(1L);
            }
            return entity;
        });
    }

    @Test
    void shouldSplitAndStoreDocument() {
        when(chunkingService.chunk("第一段\n\n第二段"))
                .thenReturn(List.of("第一段", "第二段"));

        DocumentEntity result = service.ingest(
                "Spring 笔记",
                "NOTE",
                null,
                "第一段\n\n第二段"
        );

        assertEquals("READY", result.getStatus());
        assertEquals(2, result.getChunkCount());
        assertTrue(result.getContentHash().matches("[0-9a-f]{64}"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        assertEquals("第一段", captor.getValue().get(0).getText());
        assertEquals("1", captor.getValue().get(0).getMetadata().get("documentId"));
        assertEquals(0, captor.getValue().get(0).getMetadata().get("chunkIndex"));
    }

    @Test
    void shouldMarkDocumentFailedWhenVectorStoreFails() {
        when(chunkingService.chunk("正文")).thenReturn(List.of("正文"));
        doThrow(new RuntimeException("向量服务失败")).when(vectorStore).add(anyList());

        assertThrows(
                RuntimeException.class,
                () -> service.ingest("失败资料", "txt", null, "正文")
        );

        ArgumentCaptor<DocumentEntity> captor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository, times(2)).save(captor.capture());
        assertEquals("FAILED", captor.getValue().getStatus());
    }
}
