package com.ithwx.personalknowledgebase.index.infrastructure;

import com.ithwx.personalknowledgebase.index.domain.KnowledgeChunk;
import com.ithwx.personalknowledgebase.index.domain.SearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SiliconFlowKnowledgeRerankerTest {

    @Test
    void shouldRerankCandidatesWithSiliconFlow() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SiliconFlowKnowledgeReranker reranker = new SiliconFlowKnowledgeReranker(
                builder,
                "https://api.siliconflow.cn/v1",
                "test-key",
                "BAAI/bge-reranker-v2-m3"
        );
        List<KnowledgeChunk> candidates = List.of(
                chunk(1L, "无关内容"),
                chunk(2L, "Spring Boot 数据库配置")
        );

        server.expect(requestTo("https://api.siliconflow.cn/v1/rerank"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("BAAI/bge-reranker-v2-m3"))
                .andExpect(jsonPath("$.top_n").value(1))
                .andRespond(withSuccess("""
                        {
                          "results": [
                            {"index": 1, "relevance_score": 0.96}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<SearchResult> results = reranker.rerank(
                "怎么连接数据库？", candidates, 1
        );

        assertEquals(2L, results.get(0).chunk().documentId());
        assertEquals(0.96, results.get(0).score());
        server.verify();
    }

    private KnowledgeChunk chunk(Long documentId, String text) {
        return new KnowledgeChunk(
                documentId, "资料" + documentId, "note", null,
                0, text, "Java", Set.of("Spring")
        );
    }
}
