package com.ithwx.personalknowledgebase.qa.application;

import com.ithwx.personalknowledgebase.qa.domain.Conversation;
import com.ithwx.personalknowledgebase.qa.domain.ConversationRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final AnswerQuestion answerQuestion;

    public ChatService(
            ConversationRepository conversationRepository,
            AnswerQuestion answerQuestion
    ) {
        this.conversationRepository = conversationRepository;
        this.answerQuestion = answerQuestion;
    }

    public ChatAnswer ask(Long conversationId, String question) {
        Conversation conversation = conversationId == null
                ? Conversation.start()
                : getConversation(conversationId);
        String normalizedQuestion = question.strip();
        ChatAnswer result = answerQuestion.answer(
                normalizedQuestion, conversation.messages());

        conversation.addUserMessage(normalizedQuestion);
        conversation.addAssistantMessage(
                result.answer(), result.refused(), result.sources());
        Conversation saved = conversationRepository.save(conversation);

        return result.withConversationId(saved.id());
    }

    public Conversation getConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NoSuchElementException(
                        "会话不存在：" + conversationId));
    }

}
