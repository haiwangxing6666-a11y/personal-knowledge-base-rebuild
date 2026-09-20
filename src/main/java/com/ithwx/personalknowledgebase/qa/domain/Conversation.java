package com.ithwx.personalknowledgebase.qa.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Conversation {

    private final Long id;
    private final LocalDateTime createdAt;
    private final List<ChatMessage> messages;

    public Conversation(Long id, LocalDateTime createdAt, List<ChatMessage> messages) {
        this.id = id;
        this.createdAt = createdAt;
        this.messages = new ArrayList<>(messages);
    }

    public static Conversation start() {
        return new Conversation(null, LocalDateTime.now(), List.of());
    }

    public void addUserMessage(String content) {
        messages.add(new ChatMessage(
                MessageRole.USER, content, false, List.of(), LocalDateTime.now()));
    }

    public void addAssistantMessage(
            String content,
            boolean refused,
            List<AnswerSource> sources
    ) {
        messages.add(new ChatMessage(
                MessageRole.ASSISTANT, content, refused, sources, LocalDateTime.now()));
    }

    public Long id() {
        return id;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public List<ChatMessage> messages() {
        return List.copyOf(messages);
    }
}
