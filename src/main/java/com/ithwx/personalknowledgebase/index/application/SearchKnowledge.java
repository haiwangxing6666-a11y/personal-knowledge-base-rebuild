package com.ithwx.personalknowledgebase.index.application;

import com.ithwx.personalknowledgebase.index.domain.KnowledgeIndex;
import com.ithwx.personalknowledgebase.index.domain.KnowledgeReranker;
import com.ithwx.personalknowledgebase.index.domain.SearchQuery;
import com.ithwx.personalknowledgebase.index.domain.SearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchKnowledge {

    private static final int CANDIDATE_MULTIPLIER = 2;

    private final KnowledgeIndex knowledgeIndex;
    private final KnowledgeReranker knowledgeReranker;
    private final int maxResults;
    private final int maxContextChars;
    private final double similarityThreshold;

    public SearchKnowledge(
            KnowledgeIndex knowledgeIndex,
            KnowledgeReranker knowledgeReranker,
            @Value("${app.rag.top-k}") int maxResults,
            @Value("${app.rag.max-context-chars}") int maxContextChars,
            @Value("${app.rag.similarity-threshold}") double similarityThreshold
    ) {
        this.knowledgeIndex = knowledgeIndex;
        this.knowledgeReranker = knowledgeReranker;
        this.maxResults = maxResults;
        this.maxContextChars = maxContextChars;
        this.similarityThreshold = similarityThreshold;
    }

    public List<SearchResult> search(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("检索问题不能为空");
        }

        String normalizedQuestion = question.strip();
        SearchQuery query = new SearchQuery(
                normalizedQuestion,
                maxResults * CANDIDATE_MULTIPLIER,
                similarityThreshold
        );
        return limitContext(knowledgeReranker.rerank(
                normalizedQuestion,
                knowledgeIndex.search(query),
                maxResults
        ));
    }

    private List<SearchResult> limitContext(List<SearchResult> candidates) {
        List<SearchResult> selected = new ArrayList<>();
        int contextChars = 0;

        for (SearchResult candidate : candidates) {
            int chunkChars = candidate.chunk().text().length();
            if (selected.size() == maxResults || contextChars + chunkChars > maxContextChars) {
                break;
            }
            selected.add(candidate);
            contextChars += chunkChars;
        }
        return List.copyOf(selected);
    }
}
