package com.ithwx.personalknowledgebase.controller.dto;

import com.ithwx.personalknowledgebase.practice.domain.PracticeSession;

import java.time.LocalDateTime;

public record PracticeQuestionResponse(
        Long practiceId,
        String topic,
        String question,
        String status,
        LocalDateTime createdAt
) {
    public static PracticeQuestionResponse from(PracticeSession session) {
        return new PracticeQuestionResponse(
                session.id(),
                session.topic(),
                session.question(),
                session.status().name(),
                session.createdAt()
        );
    }
}
