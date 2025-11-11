package at.technikum_wien.DocumentDAL.controller;

import at.technikum_wien.DocumentDAL.model.Document;
import at.technikum_wien.DocumentDAL.repo.DocumentRepository;
import at.technikum_wien.DocumentDAL.services.DocumentService;
import at.technikum_wien.DocumentDAL.services.PdfPreviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentRepository documentRepository;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private PdfPreviewService pdfPreviewService;

    @Autowired
    private ObjectMapper objectMapper;

    private Document testDocument;
    private List<Document> testDocuments;

    @BeforeEach
    void setUp() {
        testDocument = new Document();
        testDocument.setId(1);
        testDocument.setTitle("Test Document");
        testDocument.setSummary("Test Summary");
        testDocument.setContent("Test Content");
        testDocument.setFileName("test.pdf");
        testDocument.setMimeType("application/pdf");
        testDocument.setSize(1024L);
        testDocument.setUploadDate(LocalDateTime.now());

        Document testDocument2 = new Document();
        testDocument2.setId(2);
        testDocument2.setTitle("Second Document");
        testDocument2.setSummary("Second Summary");
        testDocument2.setFileName("test2.txt");
        testDocument2.setMimeType("text/plain");
        testDocument2.setSize(512L);
        testDocument2.setUploadDate(LocalDateTime.now());

        testDocuments = Arrays.asList(testDocument, testDocument2);

        reset(documentRepository, pdfPreviewService, documentService);
    }

    @Test
    void getAllDocuments_ShouldReturnListOfDocuments() throws Exception {
        when(documentRepository.findAll()).thenReturn(testDocuments);

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Document"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].title").value("Second Document"));

        verify(documentRepository, times(1)).findAll();
    }

    @Test
    void getDocumentById_WhenDocumentExists_ShouldReturnDocument() throws Exception {
        when(documentRepository.findById(1)).thenReturn(Optional.of(testDocument));

        mockMvc.perform(get("/api/documents/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Document"))
                .andExpect(jsonPath("$.summary").value("Test Summary"));

        verify(documentRepository, times(1)).findById(1);
    }

    @Test
    void getDocumentById_WhenDocumentDoesNotExist_ShouldReturn404() throws Exception {
        when(documentRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/documents/999"))
                .andExpect(status().isNotFound());

        verify(documentRepository, times(1)).findById(999);
    }

    @Test
    void uploadDocument_ShouldCreateNewDocument() throws Exception {
        Document newDocument = new Document();
        newDocument.setTitle("New Document");
        newDocument.setSummary("New Summary");
        newDocument.setContent("New Content");

        Document savedDocument = new Document();
        savedDocument.setId(3);
        savedDocument.setTitle("New Document");
        savedDocument.setSummary("New Summary");
        savedDocument.setContent("New Content");
        savedDocument.setUploadDate(LocalDateTime.now());

        when(documentService.createDocument(any(Document.class))).thenReturn(savedDocument);

        mockMvc.perform(post("/api/documents/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDocument)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.title").value("New Document"));

        verify(documentService, times(1)).createDocument(any(Document.class));
    }

    @Test
    void uploadFile_WithValidPdfFile_ShouldCreateDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        Document savedDocument = new Document();
        savedDocument.setId(4);
        savedDocument.setTitle("Test Title");
        savedDocument.setFileName("test.pdf");
        savedDocument.setMimeType("application/pdf");
        savedDocument.setSize(file.getSize());
        savedDocument.setUploadDate(LocalDateTime.now());

        when(documentService.create(any(), any(), any(), any())).thenReturn(savedDocument);

        mockMvc.perform(multipart("/api/documents/upload-file")
                        .file(file)
                        .param("title", "Test Title")
                        .param("summary", "Test Summary"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.fileName").value("test.pdf"));

        verify(documentService, times(1)).create(any(), any(), any(), any());
    }

    @Test
    void uploadFile_WithInvalidFileType_ShouldReturnBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.exe",
                "application/octet-stream",
                "Invalid content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload-file")
                        .file(file)
                        .param("title", "Test Title"))
                .andExpect(status().isBadRequest());

        verify(documentService, never()).create(any(), any(), any(), any());
    }

    @Test
    void uploadFile_WithOversizedFile_ShouldReturnBadRequest() throws Exception {
        byte[] largeContent = new byte[51 * 1024 * 1024]; // 51 MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                largeContent
        );

        mockMvc.perform(multipart("/api/documents/upload-file")
                        .file(file)
                        .param("title", "Large File"))
                .andExpect(status().isBadRequest());

        verify(documentService, never()).create(any(), any(), any(), any());
    }

    @Test
    void searchDocuments_WithQuery_ShouldReturnFilteredResults() throws Exception {
        when(documentRepository.searchWithoutFileData("test"))
                .thenReturn(Arrays.asList(testDocument));

        mockMvc.perform(get("/api/documents/search")
                        .param("q", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Document"));

        verify(documentRepository, times(1)).searchWithoutFileData("test");
    }

    @Test
    void searchDocuments_WithoutQuery_ShouldReturnAllDocuments() throws Exception {
        when(documentRepository.findAllWithoutFileData()).thenReturn(testDocuments);

        mockMvc.perform(get("/api/documents/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(documentRepository, times(1)).findAllWithoutFileData();
    }

    @Test
    void downloadFile_WhenDocumentExists_ShouldReturnFileData() throws Exception {
        when(documentRepository.findById(1)).thenReturn(Optional.of(testDocument));
        when(documentService.getFileBytes(1)).thenReturn("PDF content".getBytes());

        mockMvc.perform(get("/api/documents/1/file"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''test.pdf"))
                .andExpect(content().bytes("PDF content".getBytes()));

        verify(documentRepository, times(1)).findById(1);
        verify(documentService, times(1)).getFileBytes(1);
    }

    @Test
    void downloadFile_WhenDocumentDoesNotExist_ShouldReturn404() throws Exception {
        when(documentRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/documents/999/file"))
                .andExpect(status().isNotFound());

        verify(documentRepository, times(1)).findById(999);
        verify(documentService, never()).getFileBytes(anyInt());
    }

    @Test
    void deleteDocument_WhenDocumentExists_ShouldDeleteAndReturnNoContent() throws Exception {
        when(documentRepository.existsById(1)).thenReturn(true);

        mockMvc.perform(delete("/api/documents/1"))
                .andExpect(status().isNoContent());

        verify(documentRepository, times(1)).existsById(1);
        verify(documentService, times(1)).delete(1);
    }

    @Test
    void deleteDocument_WhenDocumentDoesNotExist_ShouldReturn404() throws Exception {
        when(documentRepository.existsById(999)).thenReturn(false);

        mockMvc.perform(delete("/api/documents/999"))
                .andExpect(status().isNotFound());

        verify(documentRepository, times(1)).existsById(999);
        verify(documentService, never()).delete(anyInt());
    }

    @Test
    void updateDocument_WhenDocumentExists_ShouldUpdateAndReturnDocument() throws Exception {
        Document updatedDocument = new Document();
        updatedDocument.setId(1);
        updatedDocument.setTitle("Updated Title");
        updatedDocument.setSummary("Updated Summary");
        updatedDocument.setUploadDate(testDocument.getUploadDate());

        when(documentRepository.findById(1)).thenReturn(Optional.of(testDocument));
        when(documentRepository.save(any(Document.class))).thenReturn(updatedDocument);

        mockMvc.perform(put("/api/documents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDocument)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.summary").value("Updated Summary"));

        verify(documentRepository, times(1)).findById(1);
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    void updateDocument_WhenDocumentDoesNotExist_ShouldReturn404() throws Exception {
        Document updatedDocument = new Document();
        updatedDocument.setTitle("Updated Title");

        when(documentRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/documents/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDocument)))
                .andExpect(status().isNotFound());

        verify(documentRepository, times(1)).findById(999);
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void previewDocument_WhenDocumentNotFound_ShouldReturn404() throws Exception {
        when(documentRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/documents/999/preview"))
                .andExpect(status().isNotFound());

        verify(documentRepository, times(1)).findById(999);
        verify(pdfPreviewService, never()).renderFirstPageAsPng(any(byte[].class));
        verify(documentService, never()).getFileBytes(anyInt());
    }

    @Test
    void previewDocument_WhenNotPdf_ShouldReturn404() throws Exception {
        testDocument.setMimeType("text/plain");
        when(documentRepository.findById(1)).thenReturn(Optional.of(testDocument));

        mockMvc.perform(get("/api/documents/1/preview"))
                .andExpect(status().isNotFound());

        verify(documentRepository, times(1)).findById(1);
        verify(pdfPreviewService, never()).renderFirstPageAsPng(any(byte[].class));
        verify(documentService, never()).getFileBytes(anyInt());
    }

    @Test
    void previewDocument_WhenFileMissing_ShouldReturn500() throws Exception {
        testDocument.setMimeType("application/pdf");
        when(documentRepository.findById(1)).thenReturn(Optional.of(testDocument));
        when(documentService.getFileBytes(1)).thenThrow(new IOException("missing"));

        mockMvc.perform(get("/api/documents/1/preview"))
                .andExpect(status().isInternalServerError());

        verify(documentRepository, times(1)).findById(1);
        verify(documentService, times(1)).getFileBytes(1);
        verify(pdfPreviewService, never()).renderFirstPageAsPng(any(byte[].class));
    }
}