package at.technikum_wien.ocrworker.listener;

import at.technikum_wien.ocrworker.model.DocumentUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DocumentUploadedListener {
    private static final Logger log = LoggerFactory.getLogger(DocumentUploadedListener.class);

    @RabbitListener(queuesToDeclare = @Queue(value = "${DOC_EVENTS_QUEUE:documents.uploaded}", durable = "true"))
    public void onMessage(DocumentUploadedEvent event) {
        log.info("[OCR-WORKER] Received id={} file='{}' mime={} size={}",
                event.id(), event.fileName(), event.mimeType(), event.size());
        // Verarbeitung OK => Auto-Ack
    }
}
