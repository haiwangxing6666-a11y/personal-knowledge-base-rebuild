package com.ithwx.personalknowledgebase.practice.domain;

import java.util.List;

public interface QuestionGenerator {

    GeneratedQuestion generate(String topic, List<PracticeSource> sources);
}
