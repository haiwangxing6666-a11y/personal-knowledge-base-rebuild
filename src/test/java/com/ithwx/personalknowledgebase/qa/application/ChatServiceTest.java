package com.ithwx.personalknowledgebase.qa.application;

import com.ithwx.personalknowledgebase.dto.RagAnswerResult;
import com.ithwx.personalknowledgebase.qa.domain.AnswerSource;
import com.ithwx.personalknowledgebase.qa.domain.Conversation;
import com.ithwx.personalknowledgebase.qa.domain.ConversationRepository;
import com.ithwx.personalknowledgebase.qa.domain.MessageRole;
import com.ithwx.personalknowledgebase.service.RagAnswerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private RagAnswerService ragAnswerService;

    @Test
    void shouldCreateConversationAndSaveQuestionAndAnswer() {
        ChatService service = new ChatService(conversationRepository, ragAnswerService);
        RagAnswerResult result = result();
        when(ragAnswerService.answer("支持什么格式？")).thenReturn(result);
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> {
                    Conversation conversation = invocation.getArgument(0);
                    return new Conversation(
                            10L, conversation.createdAt(), conversation.messages());
                });

        ChatAnswer answer = service.ask(null, "  支持什么格式？  ");

        assertEquals(10L, answer.conversationId());
        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertEquals(2, captor.getValue().messages().size());
        assertEquals(MessageRole.USER, captor.getValue().messages().get(0).role());
        assertEquals("支持什么格式？", captor.getValue().messages().get(0).content());
        assertEquals(MessageRole.ASSISTANT, captor.getValue().messages().get(1).role());
        assertEquals(1, captor.getValue().messages().get(1).sources().size());
    }

    @Test
    void shouldContinueExistingConversation() {
        ChatService service = new ChatService(conversationRepository, ragAnswerService);
        Conversation conversation = new Conversation(
                5L, java.time.LocalDateTime.now(), List.of());
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conversation));
        when(ragAnswerService.answer("继续提问")).thenReturn(result());
        when(conversationRepository.save(conversation)).thenReturn(conversation);

        ChatAnswer answer = service.ask(5L, "继续提问");

        assertEquals(5L, answer.conversationId());
        assertEquals(2, conversation.messages().size());
    }

    @Test
    void shouldRejectUnknownConversation() {
        ChatService service = new ChatService(conversationRepository, ragAnswerService);
        when(conversationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class,
                () -> service.getConversation(99L));
    }

    private RagAnswerResult result() {
        return new RagAnswerResult(
                "支持什么格式？",
                "支持 TXT。",
                false,
                null,
                false,
                List.of(new AnswerSource(
                        1L, "项目说明", "note", null, List.of(0)))
        );
    }
}
