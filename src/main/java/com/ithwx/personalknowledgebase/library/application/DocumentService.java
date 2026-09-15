package com.ithwx.personalknowledgebase.library.application;

import com.ithwx.personalknowledgebase.library.domain.Document;
import com.ithwx.personalknowledgebase.library.domain.DocumentDeleted;
import com.ithwx.personalknowledgebase.library.domain.DocumentMetadataUpdated;
import com.ithwx.personalknowledgebase.library.domain.DocumentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class DocumentService {

    private final DocumentRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public DocumentService(DocumentRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public List<Document> list() {
        return repository.listNewestFirst();
    }

    public Document get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("资料不存在：" + id));
    }

    public Document updateMetadata(Long id, String category, Set<String> tags) {
        Document document = get(id);
        document.setCategory(category == null || category.isBlank() ? null : category.strip());
        Set<String> normalizedTags = new LinkedHashSet<>();
        if (tags != null) {
            for (String tag : tags) {
                normalizedTags.add(tag.strip());
            }
        }
        document.setTags(normalizedTags);
        Document saved = repository.save(document);
        eventPublisher.publishEvent(new DocumentMetadataUpdated(saved.getId()));
        return saved;
    }

    public void delete(Long id) {
        Document document = get(id);
        repository.delete(document);
        eventPublisher.publishEvent(new DocumentDeleted(id));
    }
}
