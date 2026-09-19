package com.ithwx.personalknowledgebase.index.application;

import com.ithwx.personalknowledgebase.index.domain.KnowledgeIndex;
import com.ithwx.personalknowledgebase.index.domain.SearchQuery;
import com.ithwx.personalknowledgebase.index.domain.SearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class SearchKnowledge {

    private static final int CANDIDATE_MULTIPLIER = 4;

    private final KnowledgeIndex knowledgeIndex;
    private final int maxResults;
    private final int maxContextChars;
    private final double similarityThreshold;

    public SearchKnowledge(
            KnowledgeIndex knowledgeIndex,
            @Value("${app.rag.top-k}") int maxResults,
            @Value("${app.rag.max-context-chars}") int maxContextChars,
            @Value("${app.rag.similarity-threshold}") double similarityThreshold
    ) {
        this.knowledgeIndex = knowledgeIndex;
        this.maxResults = maxResults;
        this.maxContextChars = maxContextChars;
        this.similarityThreshold = similarityThreshold;
    }

    public List<SearchResult> search(String question) {
        return search(question, null, Set.of());
    }

    public List<SearchResult> search(String question, String category, Set<String> tags) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("检索问题不能为空");
        }

        SearchQuery query = new SearchQuery(
                question.strip(),
                category,
                tags,
                maxResults * CANDIDATE_MULTIPLIER,
                similarityThreshold
        );
        return limitContext(knowledgeIndex.search(query));
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
