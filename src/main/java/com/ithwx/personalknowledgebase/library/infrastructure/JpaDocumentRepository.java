package com.ithwx.personalknowledgebase.library.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface JpaDocumentRepository extends JpaRepository<DocumentEntity, Long> {
    List<DocumentEntity> findAllByOrderByUploadTimeDesc();
    boolean existsByContentHash(String hash);
    List<DocumentEntity> findAllByStatusIn(List<String> statuses);
    boolean existsByContentHashAndIdNot(String hash, Long id);
}
