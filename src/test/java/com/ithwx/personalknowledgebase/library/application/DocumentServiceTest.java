package com.ithwx.personalknowledgebase.library.application;

import com.ithwx.personalknowledgebase.library.domain.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private SubmitDocument submitDocument;
    @Mock
    private ProcessDocument processDocument;
    @Mock
    private ManageDocument manageDocument;

    @Test
    void shouldBeTheOnlyFacadeForDocumentOperations() throws Exception {
        Document document = new Document();
        DocumentService service = new DocumentService(
                submitDocument, processDocument, manageDocument);
        byte[] content = "正文".getBytes();

        when(submitDocument.file("笔记.txt", content, "Java", Set.of("基础")))
                .thenReturn(document);
        when(manageDocument.list()).thenReturn(List.of(document));
        when(processDocument.retry(1L)).thenReturn(document);

        assertSame(document, service.submitFile(
                "笔记.txt", content, "Java", Set.of("基础")));
        assertSame(document, service.list().get(0));
        assertSame(document, service.retry(1L));

        service.markReady(1L, 2);
        service.markFailed(1L, "失败");
        verify(processDocument).markReady(1L, 2);
        verify(processDocument).markFailed(1L, "失败");
    }
}
