package com.ithwx.personalknowledgebase.practice.domain;

import java.time.LocalDateTime;
import java.util.List;

public class PracticeSession {

    private static final int PASSING_SCORE = 60;

    private final Long id;
    private final String topic;
    private final String question;
    private final String referenceAnswer;
    private final String evidenceSnapshot;
    private final List<PracticeSource> sources;
    private final LocalDateTime createdAt;
    private String userAnswer;
    private Integer score;
    private String feedback;
    private boolean needsReview;
    private PracticeStatus status;

    public PracticeSession(
            Long id,
            String topic,
            String question,
            String referenceAnswer,
            String evidenceSnapshot,
            List<PracticeSource> sources,
            String userAnswer,
            Integer score,
            String feedback,
            boolean needsReview,
            PracticeStatus status,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.topic = topic;
        this.question = question;
        this.referenceAnswer = referenceAnswer;
        this.evidenceSnapshot = evidenceSnapshot;
        this.sources = List.copyOf(sources);
        this.userAnswer = userAnswer;
        this.score = score;
        this.feedback = feedback;
        this.needsReview = needsReview;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static PracticeSession start(
            String topic,
            GeneratedQuestion generatedQuestion,
            String evidenceSnapshot,
            List<PracticeSource> sources
    ) {
        return new PracticeSession(
                null,
                topic.strip(),
                generatedQuestion.question(),
                generatedQuestion.referenceAnswer(),
                evidenceSnapshot,
                sources,
                null,
                null,
                null,
                false,
                PracticeStatus.WAITING_FOR_ANSWER,
                LocalDateTime.now()
        );
    }

    public void complete(String answer, PracticeEvaluation evaluation) {
        if (status != PracticeStatus.WAITING_FOR_ANSWER) {
            throw new IllegalArgumentException("这道练习已经提交过答案");
        }
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("练习答案不能为空");
        }
        userAnswer = answer.strip();
        score = evaluation.score();
        feedback = evaluation.feedback();
        needsReview = score < PASSING_SCORE;
        status = PracticeStatus.COMPLETED;
    }

    public Long id() {
        return id;
    }

    public String topic() {
        return topic;
    }

    public String question() {
        return question;
    }

    public String referenceAnswer() {
        return referenceAnswer;
    }

    public String evidenceSnapshot() {
        return evidenceSnapshot;
    }

    public List<PracticeSource> sources() {
        return sources;
    }

    public String userAnswer() {
        return userAnswer;
    }

    public Integer score() {
        return score;
    }

    public String feedback() {
        return feedback;
    }

    public boolean needsReview() {
        return needsReview;
    }

    public PracticeStatus status() {
        return status;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }
}
