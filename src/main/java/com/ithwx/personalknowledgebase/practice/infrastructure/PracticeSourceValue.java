package com.ithwx.personalknowledgebase.practice.infrastructure;

import com.ithwx.personalknowledgebase.practice.domain.PracticeSource;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
class PracticeSourceValue {

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "document_name", nullable = false)
    private String documentName;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "source_url", length = 2048)
    private String sourceUrl;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String excerpt;

    static PracticeSourceValue from(PracticeSource source) {
        PracticeSourceValue value = new PracticeSourceValue();
        value.setDocumentId(source.documentId());
        value.setDocumentName(source.documentName());
        value.setSourceType(source.sourceType());
        value.setSourceUrl(source.sourceUrl());
        value.setChunkIndex(source.chunkIndex());
        value.setExcerpt(source.excerpt());
        return value;
    }

    PracticeSource toDomain() {
        return new PracticeSource(
                documentId,
                documentName,
                sourceType,
                sourceUrl,
                chunkIndex,
                excerpt
        );
    }
}
