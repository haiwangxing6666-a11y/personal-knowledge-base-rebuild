package com.ithwx.personalknowledgebase.practice.domain;

public record PracticeSource(
        Long documentId,
        String documentName,
        String sourceType,
        String sourceUrl,
        int chunkIndex,
        String excerpt
) {
}
