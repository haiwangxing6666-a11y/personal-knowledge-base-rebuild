package com.ithwx.personalknowledgebase.library.infrastructure;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

@Component
public class DocumentFileParser {

    private static final Set<String> SUPPORTED_TYPES =
            Set.of("txt", "md", "markdown", "pdf", "docx");

    private final Parser markdownParser = Parser.builder().build();
    private final TextContentRenderer markdownRenderer = TextContentRenderer.builder().build();

    public String fileType(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw new IllegalArgumentException("无法识别文件格式");
        }
        String type = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_TYPES.contains(type)) {
            throw new IllegalArgumentException("不支持的文件格式：" + type);
        }
        return type;
    }

    public String parse(String filename, byte[] bytes) throws IOException {
        String content = switch (fileType(filename)) {
            case "txt" -> new String(bytes, StandardCharsets.UTF_8);
            case "md", "markdown" -> parseMarkdown(bytes);
            case "pdf" -> parsePdf(bytes);
            case "docx" -> parseDocx(bytes);
            default -> throw new IllegalStateException("文件类型校验异常");
        };
        if (content.isBlank()) {
            throw new IllegalArgumentException("文件内容不能为空");
        }
        return content.strip();
    }

    private String parseMarkdown(byte[] bytes) {
        String markdown = new String(bytes, StandardCharsets.UTF_8);
        return markdownRenderer.render(markdownParser.parse(markdown));
    }

    private String parsePdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String parseDocx(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            return document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText())
                    .reduce("", (text, paragraph) -> text + paragraph + System.lineSeparator());
        }
    }
}
