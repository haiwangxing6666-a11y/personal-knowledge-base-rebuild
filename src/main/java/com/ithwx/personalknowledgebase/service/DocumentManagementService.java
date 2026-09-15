package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.dto.WebPage;
import com.ithwx.personalknowledgebase.library.domain.Document;
import com.ithwx.personalknowledgebase.library.application.DocumentService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DocumentManagementService {

    private final DocumentParserService parserService;
    private final WebContentService webContentService;
    private final KnowledgeIngestionService ingestionService;
    private final DocumentService documentService;

    public DocumentManagementService(
            DocumentParserService parserService,
            WebContentService webContentService,
            KnowledgeIngestionService ingestionService,
            DocumentService documentService
    ) {
        this.parserService = parserService;
        this.webContentService = webContentService;
        this.ingestionService = ingestionService;
        this.documentService = documentService;
    }

    public Document upload(MultipartFile file) throws IOException {
        String content = parserService.parse(file);
        String filename = file.getOriginalFilename();
        String fileType = filename.substring(filename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        return ingestionService.ingest(filename, fileType, null, content);
    }

    public Document createNote(String title, String content) {
        return ingestionService.ingest(title, "note", null, content);
    }

    public Document createLink(String url, String title) {
        WebPage page = webContentService.fetch(url);
        String documentName = title == null || title.isBlank() ? page.title() : title.strip();
        return ingestionService.ingest(documentName, "web", page.url(), page.text());
    }

    public List<Document> list() {
        return documentService.list();
    }

    public Document get(Long id) {
        return documentService.get(id);
    }

    public Document updateMetadata(Long id, String category, Set<String> tags) {
        return documentService.updateMetadata(id, category, tags);
    }

    public Document update(Long id, String name, String content) {
        Document entity = get(id);
        return ingestionService.replace(
                entity,
                name,
                entity.getFileType(),
                entity.getSourceUrl(),
                content
        );
    }

    public Document replaceFile(Long id, MultipartFile file) throws IOException {
        Document entity = get(id);
        String content = parserService.parse(file);
        String filename = file.getOriginalFilename();
        String fileType = filename.substring(filename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        return ingestionService.replace(entity, filename, fileType, null, content);
    }

    public void delete(Long id) {
        documentService.delete(id);
    }
}
