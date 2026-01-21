package at.technikum_wien.ocrworker.client;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class GenAIClientTest {

    // apiKey, endpoint, model, retries, timeout
    private final GenAIClient client = new GenAIClient(
            "fake-key",
            "http://fake",
            "gemini-test-model",
            3,
            5000L
    );

    @Test
    void extractGeminiText_ShouldExtractText_FromValidGeminiResponse() {
        // Simuliertes JSON von der Google Gemini API
        String jsonResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "Das ist " },
                      { "text": "eine Zusammenfassung." }
                    ]
                  }
                }
              ]
            }
            """;

        // Zugriff auf private Methode via Reflection
        String result = (String) ReflectionTestUtils.invokeMethod(client, "extractGeminiText", jsonResponse);

        assertThat(result).isEqualTo("Das ist eine Zusammenfassung.");
    }

    @Test
    void extractGeminiText_ShouldReturnOriginalJson_WhenStructureInvalid() {
        String invalidJson = "{ \"error\": \"something wrong\" }";

        String result = (String) ReflectionTestUtils.invokeMethod(client, "extractGeminiText", invalidJson);

        // Fallback-Verhalten: Original JSON zurückgeben
        assertThat(result).isEqualTo(invalidJson);
    }
}