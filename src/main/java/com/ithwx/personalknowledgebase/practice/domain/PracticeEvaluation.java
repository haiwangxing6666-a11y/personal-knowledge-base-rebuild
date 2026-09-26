package com.ithwx.personalknowledgebase.practice.domain;

public record PracticeEvaluation(int score, String feedback) {

    public PracticeEvaluation {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("练习得分必须在 0 到 100 之间");
        }
        if (feedback == null || feedback.isBlank()) {
            throw new IllegalArgumentException("练习反馈不能为空");
        }
        feedback = feedback.strip();
    }
}
