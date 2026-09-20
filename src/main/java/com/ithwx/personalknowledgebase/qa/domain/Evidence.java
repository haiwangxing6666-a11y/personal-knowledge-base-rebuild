package com.ithwx.personalknowledgebase.qa.domain;

public record Evidence(
        Long documentId,
        String documentName,
        String sourceType,
        String sourceUrl,
        int chunkIndex,
        String text,
        double score
) {
}
