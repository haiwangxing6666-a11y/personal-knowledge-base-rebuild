package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.dto.RetrievedChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagRetrievalServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Test
    void shouldSearchAndConvertResults() {
        RagRetrievalService service = new RagRetrievalService(vectorStore, 5, 0.55);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(document("相关内容", 0.88)));

        List<RetrievedChunk> results = service.search("  什么是 RAG？  ");

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertEquals("什么是 RAG？", captor.getValue().getQuery());
        assertEquals(5, captor.getValue().getTopK());
        assertEquals(0.55, captor.getValue().getSimilarityThreshold());

        RetrievedChunk chunk = results.get(0);
        assertEquals("相关内容", chunk.content());
        assertEquals(1L, chunk.documentId());
        assertEquals("学习笔记", chunk.documentName());
        assertEquals(0, chunk.chunkIndex());
        assertEquals(0.88, chunk.similarity());
    }

    @Test
    void shouldReturnEmptyListWhenNothingMatches() {
        RagRetrievalService service = new RagRetrievalService(vectorStore, 5, 0.55);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        assertTrue(service.search("没有答案的问题").isEmpty());
    }

    @Test
    void shouldRejectBlankQuestion() {
        RagRetrievalService service = new RagRetrievalService(vectorStore, 5, 0.55);

        assertThrows(IllegalArgumentException.class, () -> service.search("  "));
        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }

    private Document document(String text, double score) {
        return Document.builder()
                .text(text)
                .metadata(Map.of(
                        "documentId", "1",
                        "documentName", "学习笔记",
                        "sourceType", "note",
                        "chunkIndex", 0
                ))
                .score(score)
                .build();
    }
}
