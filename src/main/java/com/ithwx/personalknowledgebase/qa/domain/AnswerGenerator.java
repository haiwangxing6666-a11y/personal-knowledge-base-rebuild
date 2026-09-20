package com.ithwx.personalknowledgebase.qa.domain;

import java.util.List;

public interface AnswerGenerator {

    String generate(
            String question,
            List<ChatMessage> history,
            List<Evidence> evidence
    );
}
