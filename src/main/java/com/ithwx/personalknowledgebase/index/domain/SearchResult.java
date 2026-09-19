package com.ithwx.personalknowledgebase.index.domain;

public record SearchResult(
        KnowledgeChunk chunk,
        double score
) {
}
