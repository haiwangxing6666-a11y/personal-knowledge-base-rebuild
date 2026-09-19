package com.ithwx.personalknowledgebase.library.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Document {

    private Long id;
    private Long version;
    private String name;
    private String filePath;
    private String fileType;
    private String sourceUrl;
    private String contentHash;
    private String content;
    private String failureReason;
    private LocalDateTime uploadTime;
    private String status;
    private Integer chunkCount;
}
