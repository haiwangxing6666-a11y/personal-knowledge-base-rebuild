package com.ithwx.personalknowledgebase.practice.domain;

public record GeneratedQuestion(String question, String referenceAnswer) {

    public GeneratedQuestion {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("生成的题目不能为空");
        }
        if (referenceAnswer == null || referenceAnswer.isBlank()) {
            throw new IllegalArgumentException("参考答案不能为空");
        }
        question = question.strip();
        referenceAnswer = referenceAnswer.strip();
    }
}
