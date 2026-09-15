package com.ithwx.personalknowledgebase.library.domain;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {

    List<Document> listNewestFirst();

    Optional<Document> findById(Long id);

    boolean existsByContentHash(String hash);

    boolean existsOtherWithHash(String hash, Long documentId);

    Document save(Document document);

    void delete(Document document);
}
