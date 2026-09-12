package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.dto.RetrievedChunk;
import com.ithwx.personalknowledgebase.dto.TwoStageRetrievalResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwoStageRetrievalServiceTest {

    @Mock
    private RagRetrievalService retrievalService;
    @Mock
    private QuestionRewriteService rewriteService;

    @Test
    void shouldStopWhenFirstSearchIsEnough() {
        TwoStageRetrievalService service = new TwoStageRetrievalService(
                retrievalService, rewriteService, 2, 5
        );
        List<RetrievedChunk> chunks = List.of(chunk(1L, 0, 0.9), chunk(1L, 1, 0.8));
        when(retrievalService.search("问题")).thenReturn(chunks);

        TwoStageRetrievalResult result = service.retrieve("问题");

        assertFalse(result.secondSearchExecuted());
        assertEquals(chunks, result.chunks());
        verify(rewriteService, never()).rewrite("问题");
    }

    @Test
    void shouldRewriteSearchAndMergeResults() {
        TwoStageRetrievalService service = new TwoStageRetrievalService(
                retrievalService, rewriteService, 2, 2
        );
        when(retrievalService.search("原问题"))
                .thenReturn(List.of(chunk(1L, 0, 0.6)));
        when(rewriteService.rewrite("原问题")).thenReturn(Optional.of("改写问题"));
        when(retrievalService.search("改写问题")).thenReturn(List.of(
                chunk(1L, 0, 0.9),
                chunk(2L, 0, 0.8)
        ));

        TwoStageRetrievalResult result = service.retrieve("原问题");

        assertTrue(result.secondSearchExecuted());
        assertEquals("改写问题", result.rewrittenQuestion());
        assertEquals(List.of(0.9, 0.8),
                result.chunks().stream().map(RetrievedChunk::similarity).toList());
    }

    @Test
    void shouldKeepFirstResultsWhenRewriteFails() {
        TwoStageRetrievalService service = new TwoStageRetrievalService(
                retrievalService, rewriteService, 2, 5
        );
        List<RetrievedChunk> chunks = List.of(chunk(1L, 0, 0.6));
        when(retrievalService.search("问题")).thenReturn(chunks);
        when(rewriteService.rewrite("问题")).thenReturn(Optional.empty());

        TwoStageRetrievalResult result = service.retrieve("问题");

        assertFalse(result.secondSearchExecuted());
        assertEquals(chunks, result.chunks());
    }

    private RetrievedChunk chunk(Long documentId, int chunkIndex, double similarity) {
        return new RetrievedChunk(
                "测试内容",
                documentId,
                "测试资料",
                "note",
                null,
                chunkIndex,
                similarity
        );
    }
}
