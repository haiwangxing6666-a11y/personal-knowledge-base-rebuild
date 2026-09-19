package com.ithwx.personalknowledgebase.qa.infrastructure;

import com.ithwx.personalknowledgebase.qa.domain.AnswerSource;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
class MessageSourceValue {

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "document_name", nullable = false)
    private String documentName;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "source_url", length = 2048)
    private String sourceUrl;

    @Column(name = "chunk_indexes", nullable = false, length = 1000)
    private String chunkIndexes;

    static MessageSourceValue from(AnswerSource source) {
        MessageSourceValue value = new MessageSourceValue();
        value.setDocumentId(source.documentId());
        value.setDocumentName(source.documentName());
        value.setSourceType(source.sourceType());
        value.setSourceUrl(source.sourceUrl());
        value.setChunkIndexes(source.chunkIndexes().stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(",")));
        return value;
    }

    AnswerSource toDomain() {
        List<Integer> indexes = chunkIndexes.isBlank()
                ? List.of()
                : Arrays.stream(chunkIndexes.split(","))
                .map(Integer::valueOf)
                .toList();
        return new AnswerSource(
                documentId, documentName, sourceType, sourceUrl, indexes);
    }
}
