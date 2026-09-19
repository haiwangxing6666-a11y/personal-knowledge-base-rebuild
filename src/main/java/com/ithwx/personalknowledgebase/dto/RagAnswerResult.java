package com.ithwx.personalknowledgebase.dto;

import com.ithwx.personalknowledgebase.qa.domain.AnswerSource;

import java.util.List;

public record RagAnswerResult(
        String question,
        String answer,
        boolean refused,
        String rewrittenQuestion,
        boolean secondSearchExecuted,
        List<AnswerSource> sources
) {
}
