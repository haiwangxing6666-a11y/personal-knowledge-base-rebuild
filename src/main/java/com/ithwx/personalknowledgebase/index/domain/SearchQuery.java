package com.ithwx.personalknowledgebase.index.domain;

public record SearchQuery(
        String text,
        int candidateLimit,
        double similarityThreshold
) {
}
