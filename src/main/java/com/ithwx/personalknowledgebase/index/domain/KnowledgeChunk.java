package com.ithwx.personalknowledgebase.index.domain;

public record KnowledgeChunk(
        Long documentId,
        String documentName,
        String sourceType,
        String sourceUrl,
        int chunkIndex,
        String text
) {
}
