package com.ithwx.personalknowledgebase.index.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ithwx.personalknowledgebase.index.domain.KnowledgeChunk;
import com.ithwx.personalknowledgebase.index.domain.KnowledgeReranker;
import com.ithwx.personalknowledgebase.index.domain.SearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class SiliconFlowKnowledgeReranker implements KnowledgeReranker {

    private final RestClient restClient;
    private final String model;

    public SiliconFlowKnowledgeReranker(
            RestClient.Builder restClientBuilder,
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${app.rag.rerank-model}") String model
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.model = model;
    }

    @Override
    public List<SearchResult> rerank(
            String question,
            List<KnowledgeChunk> candidates,
            int maxResults
    ) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        RerankRequest request = new RerankRequest(
                model,
                question,
                candidates.stream().map(KnowledgeChunk::text).toList(),
                maxResults,
                false
        );
        RerankResponse response = restClient.post()
                .uri("/rerank")
                .body(request)
                .retrieve()
                .body(RerankResponse.class);

        if (response == null || response.results() == null) {
            throw new IllegalStateException("重排服务没有返回结果");
        }

        List<SearchResult> results = new ArrayList<>();
        for (RerankItem item : response.results()) {
            results.add(new SearchResult(
                    candidates.get(item.index()),
                    item.relevanceScore()
            ));
        }
        return List.copyOf(results);
    }

    private record RerankRequest(
            String model,
            String query,
            List<String> documents,
            @JsonProperty("top_n") int topN,
            @JsonProperty("return_documents") boolean returnDocuments
    ) {
    }

    private record RerankResponse(List<RerankItem> results) {
    }

    private record RerankItem(
            int index,
            @JsonProperty("relevance_score") double relevanceScore
    ) {
    }
}
