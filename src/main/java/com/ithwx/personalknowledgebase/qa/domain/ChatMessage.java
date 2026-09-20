package com.ithwx.personalknowledgebase.qa.domain;

import java.time.LocalDateTime;
import java.util.List;

public record ChatMessage(
        MessageRole role,
        String content,
        boolean refused,
        List<AnswerSource> sources,
        LocalDateTime createdAt
) {
    public ChatMessage {
        sources = List.copyOf(sources);
    }
}
