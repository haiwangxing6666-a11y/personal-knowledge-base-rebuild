package com.ithwx.personalknowledgebase.index.domain;

import java.util.List;
import java.util.Set;

public interface KnowledgeIndex {

    List<KnowledgeChunk> search(SearchQuery query);

    void replace(Long documentId, List<KnowledgeChunk> chunks);

    void updateMetadata(Long documentId, String category, Set<String> tags);

    void delete(Long documentId);
}
