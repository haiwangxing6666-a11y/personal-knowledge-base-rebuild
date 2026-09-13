package com.ithwx.personalknowledgebase.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class DocumentParserService {

    private final Parser markdownParser = Parser.builder().build();
    private final TextContentRenderer markdownRenderer = TextContentRenderer.builder().build();

    public String parse(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("无法识别文件格式");
        }

        String fileType = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        String content = switch (fileType) {
            case "txt" -> new String(file.getBytes(), StandardCharsets.UTF_8);
            case "md", "markdown" -> parseMarkdown(file);
            case "pdf" -> parsePdf(file);
            case "docx" -> parseDocx(file);
            default -> throw new IllegalArgumentException("不支持的文件格式：" + fileType);
        };

        if (content.isBlank()) {
            throw new IllegalArgumentException("文件内容不能为空");
        }
        return content;
    }

    private String parseMarkdown(MultipartFile file) throws IOException {
        String markdown = new String(file.getBytes(), StandardCharsets.UTF_8);
        return markdownRenderer.render(markdownParser.parse(markdown)).strip();
    }

    private String parsePdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String parseDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            return document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText())
                    .reduce("", (text, paragraph) -> text + paragraph + System.lineSeparator());
        }
    }
}
