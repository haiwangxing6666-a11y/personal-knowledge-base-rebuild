package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.dto.RetrievedChunk;
import com.ithwx.personalknowledgebase.index.application.SearchKnowledge;
import com.ithwx.personalknowledgebase.index.domain.KnowledgeChunk;
import com.ithwx.personalknowledgebase.index.domain.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagRetrievalServiceTest {

    @Mock
    private SearchKnowledge searchKnowledge;

    @Test
    void shouldConvertIndexResult() {
        RagRetrievalService service = new RagRetrievalService(searchKnowledge);
        KnowledgeChunk chunk = new KnowledgeChunk(
                1L, "学习笔记", "note", null,
                0, "相关内容", "Java", Set.of("RAG")
        );
        when(searchKnowledge.search("什么是 RAG？"))
                .thenReturn(List.of(new SearchResult(chunk, 0.88)));

        List<RetrievedChunk> results = service.search("什么是 RAG？");

        assertEquals("相关内容", results.get(0).content());
        assertEquals(1L, results.get(0).documentId());
        assertEquals("学习笔记", results.get(0).documentName());
        assertEquals(0, results.get(0).chunkIndex());
        assertEquals(0.88, results.get(0).similarity());
    }

    @Test
    void shouldReturnEmptyListWhenNothingMatches() {
        RagRetrievalService service = new RagRetrievalService(searchKnowledge);
        when(searchKnowledge.search("没有答案的问题")).thenReturn(List.of());

        assertTrue(service.search("没有答案的问题").isEmpty());
    }
}
