package com.ithwx.personalknowledgebase.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkingServiceTest {

    private final ChunkingService service = new ChunkingService(10, 3);

    @Test
    void shouldReturnEmptyListForBlankText() {
        assertTrue(service.chunk(null).isEmpty());
        assertTrue(service.chunk("  ").isEmpty());
    }

    @Test
    void shouldSplitAtParagraphAndKeepOverlap() {
        List<String> chunks = service.chunk("12345\n\n67890\n\nabc");

        assertEquals(List.of("12345", "345\n\n67890", "890\n\nabc"), chunks);
    }

    @Test
    void shouldHardSplitLongParagraph() {
        List<String> chunks = service.chunk("abcdefghijklmnop");

        assertEquals(List.of("abcdefghij", "hijklmnop"), chunks);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 10));
    }
}
