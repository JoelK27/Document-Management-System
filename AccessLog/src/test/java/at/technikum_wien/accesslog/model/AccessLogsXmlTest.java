package at.technikum_wien.accesslog.model;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccessLogsXmlTest {

    private final XmlMapper xmlMapper = new XmlMapper();

    @Test
    void deserialize_ShouldMapXmlToDtoCorrectly() throws Exception {
        String xmlContent = """
            <accessLogs date="2026-01-21">
                <entry documentId="1" count="50" />
                <entry documentId="2" count="120" />
            </accessLogs>
            """;

        AccessLogsXml logs = xmlMapper.readValue(xmlContent, AccessLogsXml.class);

        assertThat(logs.getDate()).isEqualTo("2026-01-21");
        assertThat(logs.getEntries()).hasSize(2);

        AccessLogsXml.LogEntry firstEntry = logs.getEntries().get(0);
        assertThat(firstEntry.getDocumentId()).isEqualTo(1);
        assertThat(firstEntry.getCount()).isEqualTo(50);
    }

    @Test
    void deserialize_ShouldHandleEmptyList() throws Exception {
        String xmlContent = "<accessLogs date='2026-01-21'></accessLogs>";

        AccessLogsXml logs = xmlMapper.readValue(xmlContent, AccessLogsXml.class);

        assertThat(logs.getDate()).isEqualTo("2026-01-21");
        // Sollte null oder leer sein, je nach Jackson Config, aber keinen Fehler werfen
    }
}