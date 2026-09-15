package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.dto.WebPage;
import com.ithwx.personalknowledgebase.library.domain.Document;
import com.ithwx.personalknowledgebase.library.application.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentManagementServiceTest {

    @Mock
    private DocumentParserService parserService;
    @Mock
    private WebContentService webContentService;
    @Mock
    private KnowledgeIngestionService ingestionService;
    @Mock
    private DocumentService documentService;

    private DocumentManagementService service;

    @BeforeEach
    void setUp() {
        service = new DocumentManagementService(
                parserService,
                webContentService,
                ingestionService,
                documentService
        );
    }

    @Test
    void shouldCreateDocumentFromFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "笔记.TXT",
                "text/plain",
                "正文".getBytes(StandardCharsets.UTF_8)
        );
        Document expected = document(1L, "笔记.TXT");
        when(parserService.parse(file)).thenReturn("正文");
        when(ingestionService.ingest("笔记.TXT", "txt", null, "正文"))
                .thenReturn(expected);

        assertSame(expected, service.upload(file));
    }

    @Test
    void shouldCreateNoteAndWebPage() {
        Document note = document(1L, "学习笔记");
        when(ingestionService.ingest("学习笔记", "note", null, "笔记正文"))
                .thenReturn(note);
        assertSame(note, service.createNote("学习笔记", "笔记正文"));

        WebPage page = new WebPage("https://example.com", "网页标题", "网页正文");
        Document web = document(2L, "网页标题");
        when(webContentService.fetch(page.url())).thenReturn(page);
        when(ingestionService.ingest("网页标题", "web", page.url(), page.text()))
                .thenReturn(web);
        assertSame(web, service.createLink(page.url(), null));
    }

    @Test
    void shouldListAndGetDocuments() {
        Document first = document(2L, "新资料");
        Document second = document(1L, "旧资料");
        when(documentService.list()).thenReturn(List.of(first, second));
        when(documentService.get(2L)).thenReturn(first);

        assertEquals(List.of(first, second), service.list());
        assertSame(first, service.get(2L));
    }

    @Test
    void shouldRejectMissingDocument() {
        when(documentService.get(99L)).thenThrow(new NoSuchElementException("资料不存在：99"));

        assertThrows(NoSuchElementException.class, () -> service.get(99L));
    }

    @Test
    void shouldUpdateDocument() {
        Document entity = document(1L, "旧名称");
        entity.setFileType("web");
        entity.setSourceUrl("https://example.com");
        when(documentService.get(1L)).thenReturn(entity);
        when(ingestionService.replace(
                entity, "新名称", "web", "https://example.com", "新正文"
        )).thenReturn(entity);

        assertSame(entity, service.update(1L, "新名称", "新正文"));
    }

    @Test
    void shouldUpdateCategoryAndTagsWithoutReindexing() {
        Document entity = document(1L, "Java 笔记");
        when(documentService.updateMetadata(1L, " Java ", Set.of(" 数据库 ")))
                .thenReturn(entity);

        assertSame(entity, service.updateMetadata(1L, " Java ", Set.of(" 数据库 ")));
    }

    @Test
    void shouldReplaceUploadedFile() throws Exception {
        Document entity = document(1L, "旧文件.txt");
        MockMultipartFile file = new MockMultipartFile(
                "file", "新文件.MD", "text/markdown",
                "# 新正文".getBytes(StandardCharsets.UTF_8)
        );
        when(documentService.get(1L)).thenReturn(entity);
        when(parserService.parse(file)).thenReturn("# 新正文");
        when(ingestionService.replace(entity, "新文件.MD", "md", null, "# 新正文"))
                .thenReturn(entity);

        assertSame(entity, service.replaceFile(1L, file));
    }

    @Test
    void shouldDelegateDeletionToLibrary() {
        service.delete(1L);

        verify(documentService).delete(1L);
    }

    private Document document(Long id, String name) {
        Document entity = new Document();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }
}
