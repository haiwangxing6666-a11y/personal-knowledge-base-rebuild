package com.ithwx.personalknowledgebase.qa.application;

import com.ithwx.personalknowledgebase.index.application.SearchKnowledge;
import com.ithwx.personalknowledgebase.index.domain.SearchResult;
import com.ithwx.personalknowledgebase.qa.domain.AnswerGenerator;
import com.ithwx.personalknowledgebase.qa.domain.AnswerSource;
import com.ithwx.personalknowledgebase.qa.domain.ChatMessage;
import com.ithwx.personalknowledgebase.qa.domain.Evidence;
import com.ithwx.personalknowledgebase.qa.domain.QuestionJudge;
import com.ithwx.personalknowledgebase.qa.domain.RetrievalDecision;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnswerQuestion {

    static final String NO_EVIDENCE_MESSAGE = "根据当前知识库资料，暂时无法回答这个问题。";

    private final SearchKnowledge searchKnowledge;
    private final QuestionJudge questionJudge;
    private final AnswerGenerator answerGenerator;
    private final int maxResults;
    private final int maxContextChars;

    public AnswerQuestion(
            SearchKnowledge searchKnowledge,
            QuestionJudge questionJudge,
            AnswerGenerator answerGenerator,
            @Value("${app.rag.top-k}") int maxResults,
            @Value("${app.rag.max-context-chars}") int maxContextChars
    ) {
        this.searchKnowledge = searchKnowledge;
        this.questionJudge = questionJudge;
        this.answerGenerator = answerGenerator;
        this.maxResults = maxResults;
        this.maxContextChars = maxContextChars;
    }

    public ChatAnswer answer(String question, List<ChatMessage> history) {
        List<Evidence> evidence = search(question);
        RetrievalDecision decision = questionJudge.decide(question, history, evidence);
        String rewrittenQuestion = null;
        boolean secondSearchExecuted = false;

        if (!decision.sufficient() && canRetry(question, decision.rewrittenQuestion())) {
            rewrittenQuestion = decision.rewrittenQuestion().strip();
            secondSearchExecuted = true;
            evidence = merge(evidence, search(rewrittenQuestion));
            decision = questionJudge.decide(question, history, evidence);
        }

        if (!decision.sufficient()) {
            return new ChatAnswer(
                    null, question, NO_EVIDENCE_MESSAGE, true,
                    rewrittenQuestion, secondSearchExecuted, List.of());
        }

        String answer = answerGenerator.generate(question, history, evidence);
        return new ChatAnswer(
                null, question, answer, false,
                rewrittenQuestion, secondSearchExecuted, collectSources(evidence));
    }

    private boolean canRetry(String question, String rewrittenQuestion) {
        return rewrittenQuestion != null
                && !rewrittenQuestion.isBlank()
                && !rewrittenQuestion.strip().equalsIgnoreCase(question);
    }

    private List<Evidence> search(String question) {
        List<Evidence> evidence = new ArrayList<>();
        for (SearchResult result : searchKnowledge.search(question)) {
            var chunk = result.chunk();
            evidence.add(new Evidence(
                    chunk.documentId(),
                    chunk.documentName(),
                    chunk.sourceType(),
                    chunk.sourceUrl(),
                    chunk.chunkIndex(),
                    chunk.text(),
                    result.score()
            ));
        }
        return evidence;
    }

    private List<Evidence> merge(List<Evidence> first, List<Evidence> second) {
        Map<String, Evidence> unique = new LinkedHashMap<>();
        for (Evidence item : first) {
            unique.put(identity(item), item);
        }
        for (Evidence item : second) {
            Evidence existing = unique.get(identity(item));
            if (existing == null || item.score() > existing.score()) {
                unique.put(identity(item), item);
            }
        }

        List<Evidence> sorted = new ArrayList<>(unique.values());
        sorted.sort(Comparator.comparingDouble(Evidence::score).reversed());

        List<Evidence> selected = new ArrayList<>();
        int contextChars = 0;
        for (Evidence item : sorted) {
            if (selected.size() == maxResults
                    || contextChars + item.text().length() > maxContextChars) {
                break;
            }
            selected.add(item);
            contextChars += item.text().length();
        }
        return List.copyOf(selected);
    }

    private String identity(Evidence evidence) {
        return evidence.documentId() + ":" + evidence.chunkIndex();
    }

    private List<AnswerSource> collectSources(List<Evidence> evidence) {
        Map<Long, SourceAccumulator> sources = new LinkedHashMap<>();
        for (Evidence item : evidence) {
            SourceAccumulator source = sources.get(item.documentId());
            if (source == null) {
                source = new SourceAccumulator(item);
                sources.put(item.documentId(), source);
            }
            source.chunkIndexes.add(item.chunkIndex());
        }

        List<AnswerSource> results = new ArrayList<>();
        for (SourceAccumulator source : sources.values()) {
            results.add(source.toAnswerSource());
        }
        return results;
    }

    private static class SourceAccumulator {

        private final Evidence firstEvidence;
        private final List<Integer> chunkIndexes = new ArrayList<>();

        private SourceAccumulator(Evidence firstEvidence) {
            this.firstEvidence = firstEvidence;
        }

        private AnswerSource toAnswerSource() {
            return new AnswerSource(
                    firstEvidence.documentId(),
                    firstEvidence.documentName(),
                    firstEvidence.sourceType(),
                    firstEvidence.sourceUrl(),
                    chunkIndexes
            );
        }
    }
}
