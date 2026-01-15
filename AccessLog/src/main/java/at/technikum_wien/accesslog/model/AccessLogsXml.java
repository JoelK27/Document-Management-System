package at.technikum_wien.accesslog.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.List;

@Data
@JacksonXmlRootElement(localName = "accessLogs")
public class AccessLogsXml {

    @JacksonXmlProperty(isAttribute = true)
    private String date;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "entry")
    private List<LogEntry> entries;

    @Data
    public static class LogEntry {
        @JacksonXmlProperty(isAttribute = true)
        private Integer documentId;

        @JacksonXmlProperty(isAttribute = true)
        private Integer count;
    }
}