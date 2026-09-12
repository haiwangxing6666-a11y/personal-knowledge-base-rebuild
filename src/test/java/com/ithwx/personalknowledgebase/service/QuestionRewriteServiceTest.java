package com.ithwx.personalknowledgebase.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionRewriteServiceTest {

    @Mock
    private ChatModel chatModel;

    @Test
    void shouldRewriteQuestion() {
        QuestionRewriteService service = new QuestionRewriteService(chatModel);
        when(chatModel.call(contains("它支持什么格式")))
                .thenReturn("个人知识库支持哪些文档格式？");

        assertEquals(
                "个人知识库支持哪些文档格式？",
                service.rewrite("它支持什么格式").orElseThrow()
        );
    }

    @Test
    void shouldReturnEmptyWhenModelFails() {
        QuestionRewriteService service = new QuestionRewriteService(chatModel);
        when(chatModel.call(contains("问题"))).thenThrow(new RuntimeException());

        assertTrue(service.rewrite("问题").isEmpty());
    }
}
