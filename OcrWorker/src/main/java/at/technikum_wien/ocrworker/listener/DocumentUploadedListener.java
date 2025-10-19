package at.technikum_wien.ocrworker.listener;

import at.technikum_wien.ocrworker.model.DocumentUploadedEvent;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DocumentUploadedListener {

    private final MinioClient minio;

    public DocumentUploadedListener(MinioClient minio) {
        this.minio = minio;
    }

    @RabbitListener(queues = "${DOC_EVENTS_QUEUE:documents.uploaded}")
    public void onMessage(DocumentUploadedEvent evt) throws Exception {
        try (var stream = minio.getObject(GetObjectArgs.builder()
                .bucket(evt.storageBucket())
                .object(evt.storageKey())
                .build())) {
            byte[] fileBytes = stream.readAllBytes();
            // TODO: OCR ausführen
        }
    }
}