package com.ithwx.personalknowledgebase.index.domain;

import java.util.List;

public interface KnowledgeReranker {

    List<SearchResult> rerank(
            String question,
            List<KnowledgeChunk> candidates,
            int maxResults
    );
}
