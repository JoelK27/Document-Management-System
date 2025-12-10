package at.technikum_wien.ocrworker.listener;

import at.technikum_wien.ocrworker.client.BackendClient;
import at.technikum_wien.ocrworker.model.DocumentUploadedEvent;
import at.technikum_wien.ocrworker.model.DocumentOcrCompletedEvent;
import at.technikum_wien.ocrworker.service.OcrService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import at.technikum_wien.ocrworker.elasticsearch.DocumentIndexRepository;
import at.technikum_wien.ocrworker.elasticsearch.DocumentIndex;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class DocumentUploadedListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadedListener.class);

    private final MinioClient minio;
    private final OcrService ocrService;
    private final BackendClient backend;
    private final RabbitTemplate rabbit;
    @Autowired
    private DocumentIndexRepository indexRepository;

    public DocumentUploadedListener(MinioClient minio, OcrService ocrService, BackendClient backend, RabbitTemplate rabbit) {
        this.minio = minio;
        this.ocrService = ocrService;
        this.backend = backend;
        this.rabbit = rabbit;
    }

    @RabbitListener(queues = "${DOC_EVENTS_QUEUE:documents.uploaded}")
    public void onMessage(DocumentUploadedEvent evt) throws Exception {
        if (evt.mimeType() == null || !evt.mimeType().equalsIgnoreCase("application/pdf")) {
            log.info("Skip non-PDF id={} mime={}", evt.id(), evt.mimeType());
            return;
        }
        log.info("OCR pipeline start id={} bucket={} key={}", evt.id(), evt.storageBucket(), evt.storageKey());

        byte[] fileBytes;
        try (var stream = minio.getObject(GetObjectArgs.builder()
                .bucket(evt.storageBucket())
                .object(evt.storageKey())
                .build())) {
            fileBytes = stream.readAllBytes();
        }

        String text = ocrService.extractPreferPdfTextThenOcr(fileBytes);
        log.info("OCR pipeline extracted {} chars for id={}", text.length(), evt.id());

        backend.updateContent(evt.id(), text);

        // Hole das vollständige Dokument aus dem Backend
        var fullDoc = backend.getDocument(evt.id());

        // Indexiere alle Felder in Elasticsearch
        indexRepository.save(new DocumentIndex(fullDoc));

        // publish OCR completed event (include extracted text or truncated version)
        var completed = new DocumentOcrCompletedEvent(evt.id(), evt.storageBucket(), evt.storageKey(), text);
        // send to queue documents.ocr.completed
        rabbit.convertAndSend("documents.ocr.completed", completed);
        log.info("Content updated in backend for id={}", evt.id());
    }
}