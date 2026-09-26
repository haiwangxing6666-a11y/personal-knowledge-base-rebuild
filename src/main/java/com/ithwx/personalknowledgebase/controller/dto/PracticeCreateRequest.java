package com.ithwx.personalknowledgebase.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PracticeCreateRequest(
        @NotBlank(message = "复习主题不能为空")
        @Size(max = 200, message = "复习主题不能超过 200 个字符")
        String topic
) {
}
