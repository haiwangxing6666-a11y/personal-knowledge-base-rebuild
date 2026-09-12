package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.dto.AnswerSource;
import com.ithwx.personalknowledgebase.dto.RagAnswerResult;
import com.ithwx.personalknowledgebase.dto.RetrievedChunk;
import com.ithwx.personalknowledgebase.dto.TwoStageRetrievalResult;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RagAnswerService {

    static final String NO_EVIDENCE_MESSAGE = "根据当前知识库资料，暂时无法回答这个问题。";

    private final TwoStageRetrievalService retrievalService;
    private final ChatModel chatModel;

    public RagAnswerService(TwoStageRetrievalService retrievalService, ChatModel chatModel) {
        this.retrievalService = retrievalService;
        this.chatModel = chatModel;
    }

    public RagAnswerResult answer(String question) {
        TwoStageRetrievalResult retrieval = retrievalService.retrieve(question);

        if (retrieval.chunks().isEmpty()) {
            return new RagAnswerResult(
                    retrieval.originalQuestion(),
                    NO_EVIDENCE_MESSAGE,
                    true,
                    retrieval.rewrittenQuestion(),
                    retrieval.secondSearchExecuted(),
                    List.of()
            );
        }

        String answer = chatModel.call(buildPrompt(
                retrieval.originalQuestion(),
                retrieval.chunks()
        ));
        return new RagAnswerResult(
                retrieval.originalQuestion(),
                answer.strip(),
                false,
                retrieval.rewrittenQuestion(),
                retrieval.secondSearchExecuted(),
                collectSources(retrieval.chunks())
        );
    }

    private String buildPrompt(String question, List<RetrievedChunk> chunks) {
        StringBuilder evidence = new StringBuilder();
        for (int index = 0; index < chunks.size(); index++) {
            evidence.append("[证据 ").append(index + 1).append("] ")
                    .append(chunks.get(index).content())
                    .append('\n');
        }

        return """
                只能依据下面的知识库证据回答问题，不得编造信息。
                回答时使用 [证据 1] 这样的编号标明依据。

                问题：%s

                证据：
                %s
                """.formatted(question, evidence);
    }

    private List<AnswerSource> collectSources(List<RetrievedChunk> chunks) {
        Map<Long, SourceAccumulator> sources = new LinkedHashMap<>();

        for (RetrievedChunk chunk : chunks) {
            SourceAccumulator source = sources.get(chunk.documentId());
            if (source == null) {
                source = new SourceAccumulator(chunk);
                sources.put(chunk.documentId(), source);
            }
            source.chunkIndexes.add(chunk.chunkIndex());
        }

        List<AnswerSource> results = new ArrayList<>();
        for (SourceAccumulator source : sources.values()) {
            results.add(source.toAnswerSource());
        }
        return results;
    }

    private static class SourceAccumulator {

        private final RetrievedChunk firstChunk;
        private final List<Integer> chunkIndexes = new ArrayList<>();

        private SourceAccumulator(RetrievedChunk firstChunk) {
            this.firstChunk = firstChunk;
        }

        private AnswerSource toAnswerSource() {
            return new AnswerSource(
                    firstChunk.documentId(),
                    firstChunk.documentName(),
                    firstChunk.sourceType(),
                    firstChunk.sourceUrl(),
                    chunkIndexes
            );
        }
    }
}
