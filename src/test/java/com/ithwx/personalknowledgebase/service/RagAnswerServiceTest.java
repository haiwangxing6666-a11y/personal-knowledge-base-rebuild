package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.dto.RagAnswerResult;
import com.ithwx.personalknowledgebase.dto.RetrievedChunk;
import com.ithwx.personalknowledgebase.dto.TwoStageRetrievalResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagAnswerServiceTest {

    @Mock
    private TwoStageRetrievalService retrievalService;
    @Mock
    private ChatModel chatModel;

    @Test
    void shouldAnswerFromEvidenceAndMergeSources() {
        RagAnswerService service = new RagAnswerService(retrievalService, chatModel);
        when(retrievalService.retrieve("支持什么格式？")).thenReturn(new TwoStageRetrievalResult(
                "支持什么格式？",
                null,
                false,
                List.of(
                        chunk(1L, 0, "支持 TXT 和 Markdown。"),
                        chunk(1L, 1, "还支持 PDF 和 DOCX。")
                )
        ));
        when(chatModel.call(contains("支持 TXT 和 Markdown")))
                .thenReturn("支持 TXT、Markdown、PDF 和 DOCX。[证据 1][证据 2]");

        RagAnswerResult result = service.answer("支持什么格式？");

        assertFalse(result.refused());
        assertEquals("支持 TXT、Markdown、PDF 和 DOCX。[证据 1][证据 2]", result.answer());
        assertEquals(1, result.sources().size());
        assertEquals(List.of(0, 1), result.sources().get(0).chunkIndexes());
    }

    @Test
    void shouldRefuseWithoutEvidenceAndSkipModel() {
        RagAnswerService service = new RagAnswerService(retrievalService, chatModel);
        when(retrievalService.retrieve("未知问题")).thenReturn(new TwoStageRetrievalResult(
                "未知问题", null, false, List.of()
        ));

        RagAnswerResult result = service.answer("未知问题");

        assertTrue(result.refused());
        assertEquals(RagAnswerService.NO_EVIDENCE_MESSAGE, result.answer());
        assertTrue(result.sources().isEmpty());
        verify(chatModel, never()).call(org.mockito.ArgumentMatchers.anyString());
    }

    private RetrievedChunk chunk(Long documentId, int chunkIndex, String content) {
        return new RetrievedChunk(
                content,
                documentId,
                "项目说明",
                "note",
                null,
                chunkIndex,
                0.9
        );
    }
}
