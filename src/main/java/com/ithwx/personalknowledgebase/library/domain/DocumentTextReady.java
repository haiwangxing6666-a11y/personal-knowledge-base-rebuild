package com.ithwx.personalknowledgebase.library.domain;

public record DocumentTextReady(
        Long documentId,
        String name,
        String sourceType,
        String sourceUrl,
        String content
) {
}
