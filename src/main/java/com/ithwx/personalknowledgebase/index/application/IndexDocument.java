package com.ithwx.personalknowledgebase.index.application;

import com.ithwx.personalknowledgebase.index.domain.KnowledgeChunk;
import com.ithwx.personalknowledgebase.index.domain.KnowledgeIndex;
import com.ithwx.personalknowledgebase.index.infrastructure.TextChunker;
import com.ithwx.personalknowledgebase.library.application.DocumentService;
import com.ithwx.personalknowledgebase.library.domain.DocumentDeleted;
import com.ithwx.personalknowledgebase.library.domain.DocumentTextReady;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IndexDocument {

    private final TextChunker textChunker;
    private final KnowledgeIndex knowledgeIndex;
    private final DocumentService documentService;

    public IndexDocument(
            TextChunker textChunker,
            KnowledgeIndex knowledgeIndex,
            DocumentService documentService
    ) {
        this.textChunker = textChunker;
        this.knowledgeIndex = knowledgeIndex;
        this.documentService = documentService;
    }

    @EventListener
    public void onTextReady(DocumentTextReady event) {
        try {
            List<String> texts = textChunker.split(event.content());
            knowledgeIndex.replace(event.documentId(), toChunks(event, texts));
            documentService.markReady(event.documentId(), texts.size());
        } catch (RuntimeException exception) {
            String reason = exception.getMessage() == null
                    ? "索引建立失败" : exception.getMessage();
            documentService.markFailed(event.documentId(), reason);
        }
    }

    @EventListener
    public void onDocumentDeleted(DocumentDeleted event) {
        knowledgeIndex.delete(event.documentId());
    }

    private List<KnowledgeChunk> toChunks(
            DocumentTextReady event,
            List<String> texts
    ) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        for (int index = 0; index < texts.size(); index++) {
            chunks.add(new KnowledgeChunk(
                    event.documentId(),
                    event.name(),
                    event.sourceType(),
                    event.sourceUrl(),
                    index,
                    texts.get(index)
            ));
        }
        return chunks;
    }
}
