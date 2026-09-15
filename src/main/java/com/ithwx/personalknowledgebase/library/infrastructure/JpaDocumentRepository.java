package com.ithwx.personalknowledgebase.library.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface JpaDocumentRepository extends JpaRepository<DocumentEntity, Long> {
    List<DocumentEntity> findAllByOrderByUploadTimeDesc();
    boolean existsByContentHash(String hash);
    boolean existsByContentHashAndIdNot(String hash, Long id);
}
