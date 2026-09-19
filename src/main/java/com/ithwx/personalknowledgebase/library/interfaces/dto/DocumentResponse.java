package com.ithwx.personalknowledgebase.library.interfaces.dto;

import com.ithwx.personalknowledgebase.library.domain.Document;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentResponse(
        Long id,
        Long version,
        String name,
        String category,
        List<String> tags,
        String fileType,
        String sourceUrl,
        String status,
        Integer chunkCount,
        String failureReason,
        LocalDateTime uploadTime
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getVersion(),
                document.getName(),
                document.getCategory(),
                List.copyOf(document.getTags()),
                document.getFileType(),
                document.getSourceUrl(),
                document.getStatus(),
                document.getChunkCount(),
                document.getFailureReason(),
                document.getUploadTime()
        );
    }
}
