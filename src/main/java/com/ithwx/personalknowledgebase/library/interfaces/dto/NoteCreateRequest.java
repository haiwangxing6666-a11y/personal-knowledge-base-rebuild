package com.ithwx.personalknowledgebase.library.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record NoteCreateRequest(
        @NotBlank(message = "笔记标题不能为空")
        @Size(max = 255, message = "笔记标题长度不能超过 255")
        String title,
        @NotBlank(message = "笔记正文不能为空")
        String content,

        @Size(max = 100, message = "分类长度不能超过 100")
        String category,

        @Size(max = 20, message = "标签数量不能超过 20")
        Set<@NotBlank(message = "标签不能为空")
            @Size(max = 50, message = "标签长度不能超过 50") String> tags
) {
}
