package com.ithwx.personalknowledgebase.library.application;

import com.ithwx.personalknowledgebase.library.domain.Document;

public interface DocumentExtractor {

    Result extract(Document document) throws Exception;

    record Result(String name, String content, String sourceUrl) {
    }
}
