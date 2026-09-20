package com.ithwx.personalknowledgebase.qa.domain;

import java.util.List;

public interface QuestionJudge {

    RetrievalDecision decide(
            String question,
            List<ChatMessage> history,
            List<Evidence> evidence
    );
}
