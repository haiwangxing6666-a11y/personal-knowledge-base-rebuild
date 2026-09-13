package com.ithwx.personalknowledgebase.controller;

import com.ithwx.personalknowledgebase.dto.RagAnswerResult;
import com.ithwx.personalknowledgebase.service.RagAnswerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private RagAnswerService ragAnswerService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ChatController(ragAnswerService))
                .build();
    }

    @Test
    void shouldReturnRagAnswer() throws Exception {
        RagAnswerResult result = new RagAnswerResult(
                "项目支持哪些格式？",
                "支持 TXT、Markdown、PDF 和 DOCX。[证据 1]",
                false,
                null,
                false,
                List.of()
        );
        when(ragAnswerService.answer("项目支持哪些格式？")).thenReturn(result);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"项目支持哪些格式？"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("支持 TXT、Markdown、PDF 和 DOCX。[证据 1]"))
                .andExpect(jsonPath("$.refused").value(false));

        verify(ragAnswerService).answer("项目支持哪些格式？");
    }

    @Test
    void shouldRejectBlankQuestion() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"  "}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ragAnswerService);
    }
}
