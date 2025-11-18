package at.technikum_wien.ocrworker.listener;

import at.technikum_wien.ocrworker.client.BackendClient;
import at.technikum_wien.ocrworker.client.GenAIClient;
import at.technikum_wien.ocrworker.exceptions.GenAiException;
import at.technikum_wien.ocrworker.model.DocumentOcrCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "GENAI_ENABLED", havingValue = "true")
public class DocumentOcrCompletedListener {
    private static final Logger log = LoggerFactory.getLogger(DocumentOcrCompletedListener.class);
    private final GenAIClient genAi;
    private final BackendClient backend;

    public DocumentOcrCompletedListener(GenAIClient genAi, BackendClient backend) {
        this.genAi = genAi;
        this.backend = backend;
    }

    @RabbitListener(queues = "${GENAI_QUEUE:documents.ocr.completed}")
    public void onMessage(DocumentOcrCompletedEvent evt) {
        String trace = "doc=" + evt.id();
        log.info("GenAI start {}", trace);
        try {
            String text = evt.extractedText();
            if (text == null || text.isBlank()) {
                log.warn("No extracted text for {}, skip", trace);
                return;
            }
            String summary = genAi.summarize(text);
            log.info("GenAI summary len={} for {}", summary != null ? summary.length() : 0, trace);
            backend.updateSummary(evt.id(), summary);
            log.info("GenAI stored summary {}", trace);
        } catch (GenAiException e) {
            // führt zu Redelivery/DLQ (siehe app.properties & Queue DLX)
            log.error("GenAI error {}: {}", trace, e.getMessage());
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("GenAI fatal {}: {}", trace, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}