package com.ithwx.personalknowledgebase.index.domain;

import java.util.Set;

public record KnowledgeChunk(
        Long documentId,
        String documentName,
        String sourceType,
        String sourceUrl,
        int chunkIndex,
        String text,
        String category,
        Set<String> tags
) {
}
