package com.ithwx.personalknowledgebase.dto;

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
        LocalDateTime uploadTime
) {
    public static DocumentResponse from(Document entity) {
        return new DocumentResponse(
                entity.getId(),
                entity.getVersion(),
                entity.getName(),
                entity.getCategory(),
                List.copyOf(entity.getTags()),
                entity.getFileType(),
                entity.getSourceUrl(),
                entity.getStatus(),
                entity.getChunkCount(),
                entity.getUploadTime()
        );
    }
}
