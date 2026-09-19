package com.ithwx.personalknowledgebase.index.infrastructure;

import com.ithwx.personalknowledgebase.index.domain.KnowledgeChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PgVectorKnowledgeIndexTest {

    @Mock
    private VectorStore vectorStore;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private PgVectorKnowledgeIndex knowledgeIndex;

    @BeforeEach
    void setUp() {
        knowledgeIndex = new PgVectorKnowledgeIndex(vectorStore, jdbcTemplate);
    }

    @Test
    void shouldReplaceVectorsWithChunkMetadata() {
        KnowledgeChunk chunk = new KnowledgeChunk(
                1L, "Spring 笔记", "note", null,
                0, "正文", "Java", Set.of("数据库"));

        knowledgeIndex.replace(1L, List.of(chunk));

        verify(vectorStore).delete(any(org.springframework.ai.vectorstore.filter.Filter.Expression.class));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<org.springframework.ai.document.Document>> documents =
                ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(documents.capture());
        assertEquals("正文", documents.getValue().get(0).getText());
        assertEquals("Java", documents.getValue().get(0).getMetadata().get("category"));
        assertEquals("数据库", documents.getValue().get(0).getMetadata().get("tags"));
    }

    @Test
    void shouldUpdateMetadata() {
        knowledgeIndex.updateMetadata(1L, "Java", Set.of("数据库"));

        verify(jdbcTemplate).update(anyString(), any(), any(), any());
    }

    @Test
    void shouldMergeAndRemoveDuplicateChunks() {
        KnowledgeChunk both = chunk(1L, 0, "同时命中");
        KnowledgeChunk vectorOnly = chunk(2L, 0, "仅向量命中");
        KnowledgeChunk keywordOnly = chunk(3L, 0, "仅关键词命中");

        List<KnowledgeChunk> results = knowledgeIndex.merge(
                List.of(both, vectorOnly),
                List.of(keywordOnly, both)
        );

        assertEquals(both, results.get(0));
        assertEquals(keywordOnly, results.get(2));
        assertEquals(3, results.size());
    }

    private KnowledgeChunk chunk(Long documentId, int chunkIndex, String text) {
        return new KnowledgeChunk(
                documentId, "资料" + documentId, "note", null,
                chunkIndex, text, "Java", Set.of("数据库")
        );
    }
}
