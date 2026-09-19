package com.ithwx.personalknowledgebase.library.infrastructure;

import com.ithwx.personalknowledgebase.library.application.DocumentExtractor;
import com.ithwx.personalknowledgebase.library.application.FileStorage;
import com.ithwx.personalknowledgebase.library.domain.Document;
import org.springframework.stereotype.Component;

@Component
public class DefaultDocumentExtractor implements DocumentExtractor {

    private final FileStorage fileStorage;
    private final DocumentFileParser fileParser;
    private final WebPageFetcher webPageFetcher;

    public DefaultDocumentExtractor(
            FileStorage fileStorage,
            DocumentFileParser fileParser,
            WebPageFetcher webPageFetcher
    ) {
        this.fileStorage = fileStorage;
        this.fileParser = fileParser;
        this.webPageFetcher = webPageFetcher;
    }

    @Override
    public Result extract(Document document) throws Exception {
        return switch (document.getFileType()) {
            case "note" -> new Result(document.getName(), document.getContent(), null);
            case "web" -> extractWebPage(document);
            default -> new Result(
                    document.getName(),
                    fileParser.parse(document.getName(), fileStorage.read(document.getFilePath())),
                    null
            );
        };
    }

    private Result extractWebPage(Document document) {
        WebPageFetcher.FetchedWebPage page = webPageFetcher.fetch(document.getSourceUrl());
        String name = document.getName().equals(document.getSourceUrl())
                ? page.title() : document.getName();
        return new Result(name, page.text(), page.url());
    }
}
