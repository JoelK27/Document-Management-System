package at.technikum_wien.ocrworker;

import at.technikum_wien.ocrworker.elasticsearch.DocumentIndexRepository;
import at.technikum_wien.ocrworker.service.OcrService;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class OcrWorkerApplicationTests {

    @MockBean
    private OcrService ocrService;

    @MockBean
    private ConnectionFactory connectionFactory;

    // Verhindert, dass Spring versucht, Elasticsearch zu kontaktieren
    @MockBean
    private DocumentIndexRepository documentIndexRepository;

    // Verhindert potenzielle MinIO-Verbindungsfehler
    @MockBean
    private MinioClient minioClient;

    @Test
    void contextLoads() {
        // Der Context fährt jetzt hoch, weil alle externen Verbindungen gemockt sind
    }
}