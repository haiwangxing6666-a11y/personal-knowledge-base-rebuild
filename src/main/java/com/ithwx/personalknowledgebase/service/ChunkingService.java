package com.ithwx.personalknowledgebase.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    private final int maxChars;
    private final int overlapChars;

    public ChunkingService(
            @Value("${app.chunk.max-chars}") int maxChars,
            @Value("${app.chunk.overlap-chars}") int overlapChars
    ) {
        if (maxChars <= 0 || overlapChars < 0 || overlapChars >= maxChars) {
            throw new IllegalArgumentException("切分长度配置不正确");
        }
        this.maxChars = maxChars;
        this.overlapChars = overlapChars;
    }

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String content = text.strip().replaceAll("\\R\\s*\\R", "\n\n");
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < content.length()) {
            int end = Math.min(start + maxChars, content.length());

            if (end < content.length()) {
                int paragraphEnd = content.lastIndexOf("\n\n", end - 1);
                if (paragraphEnd > start + overlapChars) {
                    end = paragraphEnd;
                }
            }

            chunks.add(content.substring(start, end).strip());
            if (end == content.length()) {
                break;
            }
            start = end - overlapChars;
        }

        return chunks;
    }
}
