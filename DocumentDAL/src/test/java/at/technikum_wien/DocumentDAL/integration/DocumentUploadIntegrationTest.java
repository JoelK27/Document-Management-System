package at.technikum_wien.DocumentDAL.integration;

import at.technikum_wien.DocumentDAL.elasticsearch.DocumentIndexRepository;
import at.technikum_wien.DocumentDAL.repo.DocumentRepository;
import at.technikum_wien.DocumentDAL.storage.MinioFileStorage;
import at.technikum_wien.DocumentDAL.messaging.OcrMessagePublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DocumentUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @MockBean
    private MinioFileStorage minioFileStorage;

    @MockBean
    private OcrMessagePublisher ocrMessagePublisher;

    @MockBean
    private DocumentIndexRepository documentIndexRepository;

    @Test
    void testUploadDocument() throws Exception {
        // Prepare mock file
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "integration-test.pdf",
                "application/pdf",
                "Dummy Content".getBytes()
        );

        // Mock external dependencies
        doNothing().when(minioFileStorage).put(any(), any(), any(), any());
        doNothing().when(ocrMessagePublisher).publish(any());

        // Perform Upload
        mockMvc.perform(multipart("/api/documents/upload-file")
                        .file(file)
                        .param("title", "Integration Test Doc")
                        .param("summary", "Testing upload flow"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Integration Test Doc"));

        // Verify Database State
        assert documentRepository.count() > 0;
    }
}