package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.dto.WebPage;
import com.ithwx.personalknowledgebase.entity.DocumentEntity;
import com.ithwx.personalknowledgebase.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class DocumentManagementService {

    private final DocumentParserService parserService;
    private final WebContentService webContentService;
    private final KnowledgeIngestionService ingestionService;
    private final DocumentRepository documentRepository;

    public DocumentManagementService(
            DocumentParserService parserService,
            WebContentService webContentService,
            KnowledgeIngestionService ingestionService,
            DocumentRepository documentRepository
    ) {
        this.parserService = parserService;
        this.webContentService = webContentService;
        this.ingestionService = ingestionService;
        this.documentRepository = documentRepository;
    }

    public DocumentEntity upload(MultipartFile file) throws IOException {
        String content = parserService.parse(file);
        String filename = file.getOriginalFilename();
        String fileType = filename.substring(filename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        return ingestionService.ingest(filename, fileType, null, content);
    }

    public DocumentEntity createNote(String title, String content) {
        return ingestionService.ingest(title, "note", null, content);
    }

    public DocumentEntity createLink(String url, String title) {
        WebPage page = webContentService.fetch(url);
        String documentName = title == null || title.isBlank() ? page.title() : title.strip();
        return ingestionService.ingest(documentName, "web", page.url(), page.text());
    }

    public List<DocumentEntity> list() {
        return documentRepository.findAllByOrderByUploadTimeDesc();
    }

    public DocumentEntity get(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("资料不存在：" + id));
    }
}
