package at.technikum_wien.ocrworker.listener;

import at.technikum_wien.ocrworker.model.DocumentUploadedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DocumentUploadedListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadedListener.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @RabbitListener(queues = "${DOC_EVENTS_QUEUE:documents.uploaded}")
    public void handle(byte[] payload) {
        try {
            DocumentUploadedEvent event = mapper.readValue(payload, DocumentUploadedEvent.class);
            log.info("[OCR-WORKER] Received event id={} fileName='{}' mime='{}' size={} -> (OCR pending)",
                    event.id(), event.fileName(), event.mimeType(), event.size());
        } catch (Exception e) {
            log.error("Failed to parse message: {}", e.getMessage(), e);
        }
    }
}
