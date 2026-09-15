package com.ithwx.personalknowledgebase.library.infrastructure;

import com.ithwx.personalknowledgebase.library.domain.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaDocumentRepositoryAdapterTest {

    @Mock
    private JpaDocumentRepository jpaRepository;

    @Test
    void shouldMapFieldsAndFlushBeforeReturningDocument() {
        Document input = new Document();
        input.setName("Java 笔记");
        input.setCategory("Java");
        input.setTags(Set.of("数据库"));
        input.setFileType("note");
        input.setSourceUrl("https://example.com");
        input.setContent("正文");
        input.setContentHash("abc");
        input.setStatus("READY");
        input.setChunkCount(2);

        when(jpaRepository.saveAndFlush(any(DocumentEntity.class))).thenAnswer(invocation -> {
            DocumentEntity entity = invocation.getArgument(0);
            assertEquals("Java 笔记", entity.getName());
            assertEquals("Java", entity.getCategory());
            assertEquals(Set.of("数据库"), entity.getTags());
            assertEquals("正文", entity.getContent());
            assertEquals("abc", entity.getContentHash());
            entity.setId(1L);
            entity.setVersion(0L);
            return entity;
        });

        Document saved = new JpaDocumentRepositoryAdapter(jpaRepository).save(input);

        assertEquals(1L, saved.getId());
        assertEquals(0L, saved.getVersion());
        assertEquals("Java 笔记", saved.getName());
        assertEquals("Java", saved.getCategory());
        assertEquals(Set.of("数据库"), saved.getTags());
        assertEquals("note", saved.getFileType());
        assertEquals("https://example.com", saved.getSourceUrl());
        assertEquals("正文", saved.getContent());
        assertEquals("abc", saved.getContentHash());
        assertEquals("READY", saved.getStatus());
        assertEquals(2, saved.getChunkCount());
    }
}
