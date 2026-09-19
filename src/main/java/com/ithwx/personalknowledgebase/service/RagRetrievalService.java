package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.dto.RetrievedChunk;
import com.ithwx.personalknowledgebase.index.application.SearchKnowledge;
import com.ithwx.personalknowledgebase.index.domain.SearchResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RagRetrievalService {

    private final SearchKnowledge searchKnowledge;

    public RagRetrievalService(SearchKnowledge searchKnowledge) {
        this.searchKnowledge = searchKnowledge;
    }

    public List<RetrievedChunk> search(String question) {
        List<RetrievedChunk> results = new ArrayList<>();
        for (SearchResult result : searchKnowledge.search(question)) {
            var chunk = result.chunk();
            results.add(new RetrievedChunk(
                    chunk.text(),
                    chunk.documentId(),
                    chunk.documentName(),
                    chunk.sourceType(),
                    chunk.sourceUrl(),
                    chunk.chunkIndex(),
                    result.score()
            ));
        }
        return results;
    }
}
