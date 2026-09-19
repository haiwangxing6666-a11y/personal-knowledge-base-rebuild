package com.ithwx.personalknowledgebase.index.infrastructure;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker(10, 3);

    @Test
    void shouldReturnEmptyListForBlankText() {
        assertTrue(chunker.split(null).isEmpty());
        assertTrue(chunker.split("  ").isEmpty());
    }

    @Test
    void shouldSplitAtParagraphAndKeepOverlap() {
        List<String> chunks = chunker.split("12345\n\n67890\n\nabc");

        assertEquals(List.of("12345", "345\n\n67890", "890\n\nabc"), chunks);
    }

    @Test
    void shouldHardSplitLongParagraph() {
        List<String> chunks = chunker.split("abcdefghijklmnop");

        assertEquals(List.of("abcdefghij", "hijklmnop"), chunks);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 10));
    }
}
