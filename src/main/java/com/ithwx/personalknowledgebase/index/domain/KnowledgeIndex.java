package com.ithwx.personalknowledgebase.index.domain;

import java.util.List;

public interface KnowledgeIndex {

    List<KnowledgeChunk> search(SearchQuery query);

    void replace(Long documentId, List<KnowledgeChunk> chunks);

    void delete(Long documentId);
}
