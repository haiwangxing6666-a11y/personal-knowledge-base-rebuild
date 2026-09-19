package com.ithwx.personalknowledgebase.library.domain;

import java.util.Set;

public record DocumentMetadataUpdated(
        Long documentId,
        String category,
        Set<String> tags
) {
}
