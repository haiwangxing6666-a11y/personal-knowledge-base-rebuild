package com.ithwx.personalknowledgebase.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentParserServiceTest {

    private final DocumentParserService parserService = new DocumentParserService();

    @Test
    void shouldParseTxtAndMarkdown() throws Exception {
        MockMultipartFile txt = file("note.txt", "中文笔记");
        MockMultipartFile markdown = file("README.md", "# Markdown 标题");

        assertEquals("中文笔记", parserService.parse(txt));
        assertEquals("Markdown 标题", parserService.parse(markdown));
    }

    @Test
    void shouldRemoveMarkdownFormatting() throws Exception {
        String result = parserService.parse(file(
                "note.markdown",
                "# 标题\n\n这是 **重点内容**，参考[官网](https://example.com)。"
        ));

        assertTrue(result.contains("标题"));
        assertTrue(result.contains("重点内容"));
        assertTrue(result.contains("官网"));
        assertFalse(result.contains("#"));
        assertFalse(result.contains("**"));
        assertFalse(result.contains("]("));
    }

    @Test
    void shouldParsePdfAndDocx() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "note.pdf", "application/pdf", createPdf("PDF content")
        );
        MockMultipartFile docx = new MockMultipartFile(
                "file", "note.docx", null, createDocx("DOCX content")
        );

        assertTrue(parserService.parse(pdf).contains("PDF content"));
        assertTrue(parserService.parse(docx).contains("DOCX content"));
    }

    @Test
    void shouldRecognizeUppercaseExtension() throws Exception {
        assertEquals("大写扩展名", parserService.parse(file("note.TXT", "大写扩展名")));
    }

    @Test
    void shouldRejectUnsupportedType() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> parserService.parse(file("image.jpg", "image"))
        );

        assertEquals("不支持的文件格式：jpg", exception.getMessage());
    }

    @Test
    void shouldRejectEmptyContent() {
        assertThrows(IllegalArgumentException.class, () -> parserService.parse(file("empty.txt", "")));
    }

    private MockMultipartFile file(String filename, String content) {
        return new MockMultipartFile(
                "file", filename, null, content.getBytes(StandardCharsets.UTF_8)
        );
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
