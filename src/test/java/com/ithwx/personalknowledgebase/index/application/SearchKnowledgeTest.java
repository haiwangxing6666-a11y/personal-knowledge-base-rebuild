package com.ithwx.personalknowledgebase.index.application;

import com.ithwx.personalknowledgebase.index.domain.KnowledgeChunk;
import com.ithwx.personalknowledgebase.index.domain.KnowledgeIndex;
import com.ithwx.personalknowledgebase.index.domain.SearchQuery;
import com.ithwx.personalknowledgebase.index.domain.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchKnowledgeTest {

    @Mock
    private KnowledgeIndex knowledgeIndex;

    @Test
    void shouldPassFiltersAndLimitContext() {
        SearchKnowledge service = new SearchKnowledge(knowledgeIndex, 2, 6, 0.55);
        when(knowledgeIndex.search(any())).thenReturn(List.of(
                result(1L, "一二三", 1.0),
                result(2L, "四五六", 0.8),
                result(3L, "七八九", 0.6)
        ));

        List<SearchResult> results = service.search(
                "  Spring 数据库  ", "Java", Set.of("Spring")
        );

        ArgumentCaptor<SearchQuery> captor = ArgumentCaptor.forClass(SearchQuery.class);
        verify(knowledgeIndex).search(captor.capture());
        assertEquals("Spring 数据库", captor.getValue().text());
        assertEquals("Java", captor.getValue().category());
        assertEquals(Set.of("Spring"), captor.getValue().tags());
        assertEquals(8, captor.getValue().candidateLimit());
        assertEquals(2, results.size());
    }

    @Test
    void shouldRejectBlankQuestion() {
        SearchKnowledge service = new SearchKnowledge(knowledgeIndex, 5, 3000, 0.55);

        assertThrows(IllegalArgumentException.class, () -> service.search("  "));
        verify(knowledgeIndex, never()).search(any());
    }

    private SearchResult result(Long documentId, String text, double score) {
        KnowledgeChunk chunk = new KnowledgeChunk(
                documentId, "资料" + documentId, "note", null,
                0, text, "Java", Set.of("Spring")
        );
        return new SearchResult(chunk, score);
    }
}
