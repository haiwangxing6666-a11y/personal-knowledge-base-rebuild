package com.ithwx.personalknowledgebase.library.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class Document {

    private Long id;
    private Long version;
    private String name;
    private String category;
    private Set<String> tags = new LinkedHashSet<>();
    private String filePath;
    private String fileType;
    private String sourceUrl;
    private String contentHash;
    private String content;
    private LocalDateTime uploadTime;
    private String status;
    private Integer chunkCount;
}
