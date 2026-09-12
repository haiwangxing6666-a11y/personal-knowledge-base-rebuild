package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.dto.RetrievedChunk;
import com.ithwx.personalknowledgebase.dto.TwoStageRetrievalResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TwoStageRetrievalService {

    private final RagRetrievalService retrievalService;
    private final QuestionRewriteService rewriteService;
    private final int retryMinHits;
    private final int maxResults;

    public TwoStageRetrievalService(
            RagRetrievalService retrievalService,
            QuestionRewriteService rewriteService,
            @Value("${app.rag.retry-min-hits}") int retryMinHits,
            @Value("${app.rag.top-k}") int maxResults
    ) {
        this.retrievalService = retrievalService;
        this.rewriteService = rewriteService;
        this.retryMinHits = retryMinHits;
        this.maxResults = maxResults;
    }

    public TwoStageRetrievalResult retrieve(String question) {
        List<RetrievedChunk> firstResults = retrievalService.search(question);
        String originalQuestion = question.strip();

        if (firstResults.size() >= retryMinHits) {
            return new TwoStageRetrievalResult(originalQuestion, null, false, firstResults);
        }

        String rewrittenQuestion = rewriteService.rewrite(originalQuestion)
                .filter(rewritten -> !rewritten.equalsIgnoreCase(originalQuestion))
                .orElse(null);
        if (rewrittenQuestion == null) {
            return new TwoStageRetrievalResult(originalQuestion, null, false, firstResults);
        }

        List<RetrievedChunk> secondResults = retrievalService.search(rewrittenQuestion);
        return new TwoStageRetrievalResult(
                originalQuestion,
                rewrittenQuestion,
                true,
                merge(firstResults, secondResults)
        );
    }

    private List<RetrievedChunk> merge(
            List<RetrievedChunk> firstResults,
            List<RetrievedChunk> secondResults
    ) {
        List<RetrievedChunk> allResults = new ArrayList<>(firstResults);
        allResults.addAll(secondResults);
        Map<String, RetrievedChunk> unique = new LinkedHashMap<>();

        for (RetrievedChunk chunk : allResults) {
            String key = chunk.documentId() + ":" + chunk.chunkIndex();
            RetrievedChunk existing = unique.get(key);
            if (existing == null || chunk.similarity() > existing.similarity()) {
                unique.put(key, chunk);
            }
        }

        return unique.values().stream()
                .sorted(Comparator.comparingDouble(RetrievedChunk::similarity).reversed())
                .limit(maxResults)
                .toList();
    }
}
