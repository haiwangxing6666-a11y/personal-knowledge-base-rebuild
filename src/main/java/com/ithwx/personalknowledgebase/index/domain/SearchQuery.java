package com.ithwx.personalknowledgebase.index.domain;

import java.util.Set;

public record SearchQuery(
        String text,
        String category,
        Set<String> tags,
        int candidateLimit,
        double similarityThreshold
) {

    public SearchQuery {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }
}
