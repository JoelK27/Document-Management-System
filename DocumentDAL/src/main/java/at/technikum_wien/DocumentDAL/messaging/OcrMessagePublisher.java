package at.technikum_wien.DocumentDAL.messaging;

import at.technikum_wien.DocumentDAL.messaging.events.DocumentUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OcrMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(OcrMessagePublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${DOC_EVENTS_EXCHANGE:documents.exchange}")
    private String exchange;

    @Value("${DOC_EVENTS_ROUTING_KEY:document.uploaded}")
    private String routingKey;

    public OcrMessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(DocumentUploadedEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Published DocumentUploadedEvent to exchange='{}' rk='{}' id={}", exchange, routingKey, event.id());
        } catch (Exception e) {
            log.error("Failed to publish DocumentUploadedEvent id={}: {}", event.id(), e.getMessage(), e);
        }
    }
}