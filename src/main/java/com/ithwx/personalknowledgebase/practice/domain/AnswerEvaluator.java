package com.ithwx.personalknowledgebase.practice.domain;

public interface AnswerEvaluator {

    PracticeEvaluation evaluate(PracticeSession session, String userAnswer);
}
