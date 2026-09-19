package com.ithwx.personalknowledgebase.qa.application;

import com.ithwx.personalknowledgebase.dto.RagAnswerResult;
import com.ithwx.personalknowledgebase.qa.domain.Conversation;
import com.ithwx.personalknowledgebase.qa.domain.ConversationRepository;
import com.ithwx.personalknowledgebase.service.RagAnswerService;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final RagAnswerService ragAnswerService;

    public ChatService(
            ConversationRepository conversationRepository,
            RagAnswerService ragAnswerService
    ) {
        this.conversationRepository = conversationRepository;
        this.ragAnswerService = ragAnswerService;
    }

    public ChatAnswer ask(Long conversationId, String question) {
        Conversation conversation = conversationId == null
                ? Conversation.start()
                : getConversation(conversationId);
        String normalizedQuestion = question.strip();
        RagAnswerResult result = ragAnswerService.answer(normalizedQuestion);

        conversation.addUserMessage(normalizedQuestion);
        conversation.addAssistantMessage(
                result.answer(), result.refused(), result.sources());
        Conversation saved = conversationRepository.save(conversation);

        return ChatAnswer.from(saved.id(), result);
    }

    public Conversation getConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NoSuchElementException(
                        "会话不存在：" + conversationId));
    }

}
