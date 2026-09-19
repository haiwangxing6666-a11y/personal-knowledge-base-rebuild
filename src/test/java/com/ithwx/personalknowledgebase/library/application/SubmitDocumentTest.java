package com.ithwx.personalknowledgebase.library.application;

import com.ithwx.personalknowledgebase.library.domain.Document;
import com.ithwx.personalknowledgebase.library.domain.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitDocumentTest {

    @Mock
    private DocumentRepository repository;
    @Mock
    private FileStorage fileStorage;
    @Mock
    private ProcessDocument processDocument;

    private SubmitDocument submitDocument;

    @BeforeEach
    void setUp() {
        submitDocument = new SubmitDocument(repository, fileStorage, processDocument);
        when(repository.save(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(1L);
            return document;
        });
    }

    @Test
    void shouldSaveFileAsPendingBeforeProcessing() throws Exception {
        byte[] content = "正文".getBytes();
        when(fileStorage.save("笔记.TXT", content)).thenReturn("saved.txt");

        Document result = submitDocument.file("笔记.TXT", content);

        assertEquals("PENDING", result.getStatus());
        assertEquals("txt", result.getFileType());
        assertEquals("saved.txt", result.getFilePath());
        verify(processDocument).processAsync(1L);
    }

    @Test
    void shouldSaveNoteAndWebAddressWithoutWaitingForExtraction() {
        Document note = submitDocument.note("学习笔记", "正文");
        Document web = submitDocument.webPage("https://example.com", null);

        assertEquals("正文", note.getContent());
        assertEquals("PENDING", note.getStatus());
        assertEquals("https://example.com", web.getSourceUrl());
        assertEquals("PENDING", web.getStatus());
    }
}
