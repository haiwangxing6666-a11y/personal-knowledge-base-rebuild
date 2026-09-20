package com.ithwx.personalknowledgebase.qa.domain;

public record RetrievalDecision(
        boolean sufficient,
        String rewrittenQuestion
) {

    public static RetrievalDecision enough() {
        return new RetrievalDecision(true, null);
    }

    public static RetrievalDecision retry(String rewrittenQuestion) {
        return new RetrievalDecision(false, rewrittenQuestion);
    }
}
