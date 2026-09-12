package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.dto.RetrievedChunk;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RagRetrievalService {

    private final VectorStore vectorStore;
    private final int topK;
    private final double similarityThreshold;

    public RagRetrievalService(
            VectorStore vectorStore,
            @Value("${app.rag.top-k}") int topK,
            @Value("${app.rag.similarity-threshold}") double similarityThreshold
    ) {
        this.vectorStore = vectorStore;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    public List<RetrievedChunk> search(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("检索问题不能为空");
        }

        SearchRequest request = SearchRequest.builder()
                .query(question.strip())
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();

        List<RetrievedChunk> results = new ArrayList<>();
        for (Document document : vectorStore.similaritySearch(request)) {
            Map<String, Object> metadata = document.getMetadata();
            results.add(new RetrievedChunk(
                    document.getText(),
                    Long.valueOf((String) metadata.get("documentId")),
                    (String) metadata.get("documentName"),
                    (String) metadata.get("sourceType"),
                    (String) metadata.get("sourceUrl"),
                    (Integer) metadata.get("chunkIndex"),
                    document.getScore()
            ));
        }
        return results;
    }
}
