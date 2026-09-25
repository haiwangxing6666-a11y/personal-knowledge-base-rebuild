package com.ithwx.personalknowledgebase.controller.dto;

import com.ithwx.personalknowledgebase.qa.domain.AnswerSource;
import com.ithwx.personalknowledgebase.qa.domain.ChatMessage;
import com.ithwx.personalknowledgebase.qa.domain.Conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public record ConversationResponse(
        Long id,
        List<Message> messages
) {
    public static ConversationResponse from(Conversation conversation) {
        return new ConversationResponse(
                conversation.id(),
                conversation.messages().stream().map(Message::from).toList()
        );
    }

    public record Message(
            String role,
            String content,
            boolean refused,
            List<AnswerSource> sources,
            LocalDateTime createdAt
    ) {
        private static Message from(ChatMessage message) {
            return new Message(
                    message.role().name().toLowerCase(Locale.ROOT),
                    message.content(),
                    message.refused(),
                    message.sources(),
                    message.createdAt()
            );
        }
    }
}
