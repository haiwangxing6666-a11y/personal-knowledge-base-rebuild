package com.ithwx.personalknowledgebase.library.application;

import com.ithwx.personalknowledgebase.library.domain.Document;
import com.ithwx.personalknowledgebase.library.domain.DocumentRepository;
import com.ithwx.personalknowledgebase.library.domain.DocumentStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
class SubmitDocument {

    private static final Set<String> SUPPORTED_FILE_TYPES =
            Set.of("txt", "md", "markdown", "pdf", "docx");

    private final DocumentRepository repository;
    private final FileStorage fileStorage;
    private final ProcessDocument processDocument;

    SubmitDocument(
            DocumentRepository repository,
            FileStorage fileStorage,
            ProcessDocument processDocument
    ) {
        this.repository = repository;
        this.fileStorage = fileStorage;
        this.processDocument = processDocument;
    }

    Document file(
            String filename,
            byte[] content,
            String category,
            Set<String> tags
    ) throws IOException {
        if (content.length == 0) {
            throw new IllegalArgumentException("文件内容不能为空");
        }
        Document document = pending(filename, fileType(filename), category, tags);
        document.setFilePath(fileStorage.save(filename, content));
        return saveAndProcess(document);
    }

    Document note(String title, String content, String category, Set<String> tags) {
        Document document = pending(title, "note", category, tags);
        document.setContent(content);
        return saveAndProcess(document);
    }

    Document webPage(String url, String title, String category, Set<String> tags) {
        String name = title == null || title.isBlank() ? url.strip() : title.strip();
        Document document = pending(name, "web", category, tags);
        document.setSourceUrl(url.strip());
        return saveAndProcess(document);
    }

    private Document pending(String name, String type, String category, Set<String> tags) {
        Document document = new Document();
        document.setName(name.strip());
        document.setFileType(type);
        document.setCategory(category == null || category.isBlank() ? null : category.strip());
        document.setTags(normalizeTags(tags));
        document.setStatus(DocumentStatus.PENDING.name());
        document.setChunkCount(0);
        return document;
    }

    private Document saveAndProcess(Document document) {
        Document saved = repository.save(document);
        processDocument.processAsync(saved.getId());
        return saved;
    }

    private Set<String> normalizeTags(Set<String> tags) {
        Set<String> normalized = new LinkedHashSet<>();
        if (tags != null) {
            for (String tag : tags) {
                normalized.add(tag.strip());
            }
        }
        return normalized;
    }

    private String fileType(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw new IllegalArgumentException("无法识别文件格式");
        }
        String type = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_FILE_TYPES.contains(type)) {
            throw new IllegalArgumentException("不支持的文件格式：" + type);
        }
        return type;
    }
}
