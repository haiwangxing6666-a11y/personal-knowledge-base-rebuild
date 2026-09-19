package com.ithwx.personalknowledgebase.library.application;

import com.ithwx.personalknowledgebase.library.domain.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
public class DocumentService {

    private final SubmitDocument submitDocument;
    private final ProcessDocument processDocument;
    private final ManageDocument manageDocument;

    public DocumentService(
            SubmitDocument submitDocument,
            ProcessDocument processDocument,
            ManageDocument manageDocument
    ) {
        this.submitDocument = submitDocument;
        this.processDocument = processDocument;
        this.manageDocument = manageDocument;
    }

    public Document submitFile(
            String filename,
            byte[] content,
            String category,
            Set<String> tags
    ) throws IOException {
        return submitDocument.file(filename, content, category, tags);
    }

    public Document createNote(
            String title,
            String content,
            String category,
            Set<String> tags
    ) {
        return submitDocument.note(title, content, category, tags);
    }

    public Document collectWebPage(
            String url,
            String title,
            String category,
            Set<String> tags
    ) {
        return submitDocument.webPage(url, title, category, tags);
    }

    public List<Document> list() {
        return manageDocument.list();
    }

    public Document get(Long id) {
        return manageDocument.get(id);
    }

    public Document updateMetadata(Long id, String category, Set<String> tags) {
        return manageDocument.updateMetadata(id, category, tags);
    }

    public Document update(Long id, String name, String content) {
        return manageDocument.update(id, name, content);
    }

    public Document replaceFile(Long id, String filename, byte[] content) throws IOException {
        return manageDocument.replaceFile(id, filename, content);
    }

    public void delete(Long id) {
        manageDocument.delete(id);
    }

    public Document retry(Long id) {
        return processDocument.retry(id);
    }

    public void markReady(Long id, int chunkCount) {
        processDocument.markReady(id, chunkCount);
    }

    public void markFailed(Long id, String reason) {
        processDocument.markFailed(id, reason);
    }
}
