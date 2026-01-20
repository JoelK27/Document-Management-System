package at.technikum_wien.ocrworker;

import at.technikum_wien.ocrworker.service.OcrService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class OcrWorkerApplicationTests {

    @MockBean
    private OcrService ocrService;

    @Test
    void contextLoads() {
    }

}
