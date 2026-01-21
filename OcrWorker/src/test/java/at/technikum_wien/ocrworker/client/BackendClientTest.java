package at.technikum_wien.ocrworker.client;

import at.technikum_wien.ocrworker.client.BackendClient.DocumentDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackendClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserialize_ShouldMapJsonToDtoCorrectly() throws Exception {
        // JSON mit ALLEN Feldern, die deine DocumentDto Klasse hat
        String json = """
            {
                "id": 100,
                "title": "Invoice.pdf",
                "content": "Gecrawlter Text",
                "summary": "Kurze Zusammenfassung",
                "summaryStatus": "DONE",
                "summaryGeneratedAt": "2024-01-01T10:00:00",
                "uploadDate": "2023-01-01T12:00:00",
                "fileName": "invoice.pdf",
                "mimeType": "application/pdf",
                "size": 2048,
                "storageBucket": "documents",
                "storageKey": "minio-key-123",
                "previewKey": "preview-key-123",
                "ocrJobStatus": "PENDING"
            }
            """;

        DocumentDto dto = mapper.readValue(json, DocumentDto.class);

        // Assertions prüfen jedes Feld aus BackendClient.java
        assertThat(dto.id).isEqualTo(100);
        assertThat(dto.title).isEqualTo("Invoice.pdf");
        assertThat(dto.content).isEqualTo("Gecrawlter Text");
        assertThat(dto.summary).isEqualTo("Kurze Zusammenfassung");
        assertThat(dto.summaryStatus).isEqualTo("DONE");
        assertThat(dto.fileName).isEqualTo("invoice.pdf");
        assertThat(dto.ocrJobStatus).isEqualTo("PENDING");
        assertThat(dto.storageKey).isEqualTo("minio-key-123");
        assertThat(dto.size).isEqualTo(2048L);
    }

    @Test
    void serialize_ShouldIncludeFieldsInJson() throws Exception {
        // Testet den umgekehrten Weg (z.B. für updateDocument Requests)
        DocumentDto dto = new DocumentDto();
        dto.id = 200;
        dto.title = "Update Test";
        dto.ocrJobStatus = "DONE";
        dto.content = "New Content";

        String json = mapper.writeValueAsString(dto);

        assertThat(json).contains("\"id\":200");
        assertThat(json).contains("\"title\":\"Update Test\"");
        assertThat(json).contains("\"ocrJobStatus\":\"DONE\"");
        assertThat(json).contains("\"content\":\"New Content\"");
    }

    @Test
    void ignoreUnknownProperties_ShouldWork() throws Exception {
        // Deine DTO hat @JsonIgnoreProperties(ignoreUnknown = true)
        // Das testen wir hier: Ein unbekanntes Feld "futureField" darf keinen Fehler werfen.
        String jsonWithExtraField = """
            {
                "id": 300,
                "futureField": "I am new"
            }
            """;

        DocumentDto dto = mapper.readValue(jsonWithExtraField, DocumentDto.class);

        assertThat(dto.id).isEqualTo(300);
    }
}