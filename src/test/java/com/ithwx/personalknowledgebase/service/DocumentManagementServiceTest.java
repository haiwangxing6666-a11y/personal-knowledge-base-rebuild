package com.ithwx.personalknowledgebase.service;

import com.ithwx.personalknowledgebase.dto.WebPage;
import com.ithwx.personalknowledgebase.entity.DocumentEntity;
import com.ithwx.personalknowledgebase.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
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
    private DocumentRepository documentRepository;

    private DocumentManagementService service;

    @BeforeEach
    void setUp() {
        service = new DocumentManagementService(
                parserService,
                webContentService,
                ingestionService,
                documentRepository
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
        DocumentEntity expected = document(1L, "笔记.TXT");
        when(parserService.parse(file)).thenReturn("正文");
        when(ingestionService.ingest("笔记.TXT", "txt", null, "正文"))
                .thenReturn(expected);

        assertSame(expected, service.upload(file));
    }

    @Test
    void shouldCreateNoteAndWebPage() {
        DocumentEntity note = document(1L, "学习笔记");
        when(ingestionService.ingest("学习笔记", "note", null, "笔记正文"))
                .thenReturn(note);
        assertSame(note, service.createNote("学习笔记", "笔记正文"));

        WebPage page = new WebPage("https://example.com", "网页标题", "网页正文");
        DocumentEntity web = document(2L, "网页标题");
        when(webContentService.fetch(page.url())).thenReturn(page);
        when(ingestionService.ingest("网页标题", "web", page.url(), page.text()))
                .thenReturn(web);
        assertSame(web, service.createLink(page.url(), null));
    }

    @Test
    void shouldListAndGetDocuments() {
        DocumentEntity first = document(2L, "新资料");
        DocumentEntity second = document(1L, "旧资料");
        when(documentRepository.findAllByOrderByUploadTimeDesc())
                .thenReturn(List.of(first, second));
        when(documentRepository.findById(2L)).thenReturn(Optional.of(first));

        assertEquals(List.of(first, second), service.list());
        assertSame(first, service.get(2L));
    }

    @Test
    void shouldRejectMissingDocument() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.get(99L));
    }

    @Test
    void shouldUpdateDocument() {
        DocumentEntity entity = document(1L, "旧名称");
        entity.setFileType("web");
        entity.setSourceUrl("https://example.com");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(ingestionService.replace(
                entity, "新名称", "web", "https://example.com", "新正文"
        )).thenReturn(entity);

        assertSame(entity, service.update(1L, "新名称", "新正文"));
    }

    @Test
    void shouldReplaceUploadedFile() throws Exception {
        DocumentEntity entity = document(1L, "旧文件.txt");
        MockMultipartFile file = new MockMultipartFile(
                "file", "新文件.MD", "text/markdown",
                "# 新正文".getBytes(StandardCharsets.UTF_8)
        );
        when(documentRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(parserService.parse(file)).thenReturn("# 新正文");
        when(ingestionService.replace(entity, "新文件.MD", "md", null, "# 新正文"))
                .thenReturn(entity);

        assertSame(entity, service.replaceFile(1L, file));
    }

    @Test
    void shouldDeleteVectorsBeforeDocument() {
        DocumentEntity entity = document(1L, "待删除资料");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(entity));

        service.delete(1L);

        InOrder order = inOrder(ingestionService, documentRepository);
        order.verify(ingestionService).deleteVectors(1L);
        order.verify(documentRepository).delete(entity);
    }

    private DocumentEntity document(Long id, String name) {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }
}
