package at.technikum_wien.DocumentDAL.controller;

import at.technikum_wien.DocumentDAL.elasticsearch.DocumentIndex; 
import at.technikum_wien.DocumentDAL.elasticsearch.DocumentIndexRepository;
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
    private DocumentIndexRepository documentIndexRepository;

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

        reset(documentRepository, pdfPreviewService, documentService, documentIndexRepository);
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
                .andExpect(jsonPath("$[0].title").value("Test Document"));

        verify(documentRepository, times(1)).findAll();
    }

    @Test
    void getDocumentById_WhenDocumentExists_ShouldReturnDocument() throws Exception {
        when(documentRepository.findById(1)).thenReturn(Optional.of(testDocument));

        mockMvc.perform(get("/api/documents/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1));

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

        Document savedDocument = new Document();
        savedDocument.setId(3);
        savedDocument.setTitle("New Document");
        savedDocument.setUploadDate(LocalDateTime.now());

        when(documentService.createDocument(any(Document.class))).thenReturn(savedDocument);

        mockMvc.perform(post("/api/documents/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDocument)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.title").value("New Document"));
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

        when(documentService.create(any(), any(), any(), any())).thenReturn(savedDocument);

        mockMvc.perform(multipart("/api/documents/upload-file")
                        .file(file)
                        .param("title", "Test Title")
                        .param("summary", "Test Summary"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.title").value("Test Title"));

        verify(documentService, times(1)).create(any(), any(), any(), any());
    }

    @Test
    void uploadFile_WithInvalidFileType_ShouldReturnBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.exe", "application/octet-stream", "Invalid".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload-file")
                        .file(file)
                        .param("title", "Test Title"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadFile_WithOversizedFile_ShouldReturnBadRequest() throws Exception {
        byte[] largeContent = new byte[51 * 1024 * 1024]; // 51 MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largeContent
        );

        mockMvc.perform(multipart("/api/documents/upload-file")
                        .file(file)
                        .param("title", "Large File"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchDocuments_WithQuery_ShouldReturnFilteredResults() throws Exception {
        DocumentIndex docIndex = new DocumentIndex();
        docIndex.setTitle("Test Document");
        docIndex.setSummary("Test content summary");

        when(documentIndexRepository.search("test"))
                .thenReturn(Arrays.asList(docIndex));

        mockMvc.perform(get("/api/documents/search")
                        .param("q", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Document"));

        verify(documentIndexRepository, times(1)).search("test");
    }

    @Test
    void searchDocuments_WithoutQuery_ShouldReturnAllDocuments() throws Exception {
        DocumentIndex doc1 = new DocumentIndex();
        doc1.setTitle("Doc 1");
        DocumentIndex doc2 = new DocumentIndex();
        doc2.setTitle("Doc 2");

        when(documentIndexRepository.findAll()).thenReturn(Arrays.asList(doc1, doc2));

        mockMvc.perform(get("/api/documents/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(documentIndexRepository, times(1)).findAll();
    }

    @Test
    void downloadFile_WhenDocumentExists_ShouldReturnFileData() throws Exception {
        when(documentRepository.findById(1)).thenReturn(Optional.of(testDocument));
        when(documentService.getFileBytes(1)).thenReturn("PDF content".getBytes());

        mockMvc.perform(get("/api/documents/1/file"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes("PDF content".getBytes()));
    }

    @Test
    void downloadFile_WhenDocumentDoesNotExist_ShouldReturn404() throws Exception {
        when(documentRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/documents/999/file"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDocument_WhenDocumentExists_ShouldDeleteAndReturnNoContent() throws Exception {
        when(documentRepository.existsById(1)).thenReturn(true);

        mockMvc.perform(delete("/api/documents/1"))
                .andExpect(status().isNoContent());

        verify(documentService, times(1)).delete(1);
    }

    @Test
    void deleteDocument_WhenDocumentDoesNotExist_ShouldReturn404() throws Exception {
        when(documentRepository.existsById(999)).thenReturn(false);

        mockMvc.perform(delete("/api/documents/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDocument_WhenDocumentExists_ShouldUpdateAndReturnDocument() throws Exception {
        Document updatedDocument = new Document();
        updatedDocument.setId(1);
        updatedDocument.setTitle("Updated Title");

        when(documentRepository.findById(1)).thenReturn(Optional.of(testDocument));
        when(documentRepository.save(any(Document.class))).thenReturn(updatedDocument);

        mockMvc.perform(put("/api/documents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDocument)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
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
    }

    @Test
    void previewDocument_WhenDocumentNotFound_ShouldReturn404() throws Exception {
        when(documentRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/documents/999/preview"))
                .andExpect(status().isNotFound());
    }

    @Test
    void previewDocument_WhenNotPdf_ShouldReturn404() throws Exception {
        testDocument.setMimeType("text/plain");
        when(documentRepository.findById(1)).thenReturn(Optional.of(testDocument));

        mockMvc.perform(get("/api/documents/1/preview"))
                .andExpect(status().isNotFound());
    }

    @Test
    void previewDocument_WhenFileMissing_ShouldReturn500() throws Exception {
        testDocument.setMimeType("application/pdf");
        when(documentRepository.findById(1)).thenReturn(Optional.of(testDocument));

        when(documentService.getFileBytes(1)).thenThrow(new RuntimeException("missing"));

        mockMvc.perform(get("/api/documents/1/preview"))
                .andExpect(status().isInternalServerError());
    }
}