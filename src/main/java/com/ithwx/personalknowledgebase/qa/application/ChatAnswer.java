package com.ithwx.personalknowledgebase.qa.application;

import com.ithwx.personalknowledgebase.dto.RagAnswerResult;
import com.ithwx.personalknowledgebase.qa.domain.AnswerSource;

import java.util.List;

public record ChatAnswer(
        Long conversationId,
        String question,
        String answer,
        boolean refused,
        String rewrittenQuestion,
        boolean secondSearchExecuted,
        List<AnswerSource> sources
) {
    public static ChatAnswer from(Long conversationId, RagAnswerResult result) {
        return new ChatAnswer(
                conversationId,
                result.question(),
                result.answer(),
                result.refused(),
                result.rewrittenQuestion(),
                result.secondSearchExecuted(),
                result.sources()
        );
    }
}
