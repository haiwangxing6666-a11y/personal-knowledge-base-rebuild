package com.ithwx.personalknowledgebase.library.infrastructure;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentFileParserTest {

    private final DocumentFileParser parser = new DocumentFileParser();

    @Test
    void shouldParseTxtAndMarkdown() throws Exception {
        assertEquals("中文笔记", parser.parse("note.txt", bytes("中文笔记")));
        assertEquals("Markdown 标题", parser.parse("README.md", bytes("# Markdown 标题")));
    }

    @Test
    void shouldRemoveMarkdownFormatting() throws Exception {
        String result = parser.parse(
                "note.markdown",
                bytes("# 标题\n\n这是 **重点内容**，参考[官网](https://example.com)。")
        );

        assertTrue(result.contains("标题"));
        assertTrue(result.contains("重点内容"));
        assertTrue(result.contains("官网"));
        assertFalse(result.contains("#"));
        assertFalse(result.contains("**"));
        assertFalse(result.contains("]("));
    }

    @Test
    void shouldParsePdfAndDocx() throws Exception {
        assertTrue(parser.parse("note.pdf", createPdf("PDF content")).contains("PDF content"));
        assertTrue(parser.parse("note.docx", createDocx("DOCX content")).contains("DOCX content"));
    }

    @Test
    void shouldRecognizeUppercaseExtension() throws Exception {
        assertEquals("大写扩展名", parser.parse("note.TXT", bytes("大写扩展名")));
    }

    @Test
    void shouldRejectUnsupportedOrEmptyFile() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse("image.jpg", bytes("image"))
        );
        assertEquals("不支持的文件格式：jpg", exception.getMessage());
        assertThrows(IllegalArgumentException.class, () -> parser.parse("empty.txt", new byte[0]));
    }

    private byte[] bytes(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] createPdf(String text) throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 720);
                content.showText(text);
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] createDocx(String text) throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText(text);
            document.write(output);
            return output.toByteArray();
        }
    }
}
