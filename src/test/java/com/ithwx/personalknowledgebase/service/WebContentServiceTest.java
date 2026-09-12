package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.dto.WebPage;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebContentServiceTest {

    private final WebContentService service = new WebContentService();

    @Test
    void shouldExtractTitleAndMainContent() {
        String html = """
                <html><head><title>学习笔记</title></head><body>
                <nav>导航</nav><main><h1>Java</h1><p>这是正文。</p></main>
                <script>脚本</script></body></html>
                """;

        WebPage page = service.parseContent(
                URI.create("https://example.com/note"),
                "text/html; charset=UTF-8",
                html.getBytes(StandardCharsets.UTF_8)
        );

        assertEquals("学习笔记", page.title());
        assertEquals("Java 这是正文。", page.text());
        assertFalse(page.text().contains("导航"));
        assertFalse(page.text().contains("脚本"));
    }

    @Test
    void shouldExtractPlainText() {
        WebPage page = service.parseContent(
                URI.create("https://example.com/note.txt"),
                "text/plain",
                "  纯文本内容  ".getBytes(StandardCharsets.UTF_8)
        );

        assertEquals("example.com", page.title());
        assertEquals("纯文本内容", page.text());
    }

    @Test
    void shouldRejectUnsupportedContentType() {
        assertThrows(IllegalArgumentException.class, () -> service.parseContent(
                URI.create("https://example.com/image"),
                "image/png",
                new byte[]{1}
        ));
    }

    @Test
    void shouldRejectInvalidOrPrivateUrl() {
        assertThrows(IllegalArgumentException.class, () -> service.fetch("file:///test.txt"));
        assertThrows(IllegalArgumentException.class, () -> service.fetch("http://127.0.0.1/test"));
    }
}
