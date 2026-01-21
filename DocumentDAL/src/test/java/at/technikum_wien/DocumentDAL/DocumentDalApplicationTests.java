package at.technikum_wien.DocumentDAL;

import at.technikum_wien.DocumentDAL.elasticsearch.DocumentIndexRepository; // WICHTIG: Import hinzufügen
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DocumentDalApplicationTests {

	@MockBean
	private ConnectionFactory connectionFactory;

	@MockBean
	private DocumentIndexRepository documentIndexRepository;

	@MockBean
	private MinioClient minioClient;

	@Test
	void contextLoads() {
		// Jetzt lädt der Context, weil das Repository durch einen simplen Mock ersetzt ist
		// und Spring nicht versucht, eine echte Elasticsearch-Verbindung aufzubauen.
	}

}