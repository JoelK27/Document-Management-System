package at.technikum_wien.DocumentDAL.services;

import at.technikum_wien.DocumentDAL.elasticsearch.DocumentIndex;
import at.technikum_wien.DocumentDAL.elasticsearch.DocumentIndexRepository;
import at.technikum_wien.DocumentDAL.exceptions.DocumentNotFoundException;
import at.technikum_wien.DocumentDAL.exceptions.FileValidationException;
import at.technikum_wien.DocumentDAL.messaging.OcrMessagePublisher;
import at.technikum_wien.DocumentDAL.messaging.events.DocumentUploadedEvent;
import at.technikum_wien.DocumentDAL.model.Document;
import at.technikum_wien.DocumentDAL.repo.DocumentRepository;
import at.technikum_wien.DocumentDAL.storage.MinioFileStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentService {

    private static final String SUMMARY_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String SUMMARY_STATUS_DONE = "GENAI_DONE";
    private static final String SUMMARY_STATUS_FAILED = "GENAI_FAILED";

    private final DocumentRepository repo;
    private final OcrMessagePublisher publisher;
    private final MinioFileStorage storage;
    private final String documentBucket;

    @Autowired(required = false)
    private DocumentIndexRepository elasticRepo;

    public DocumentService(DocumentRepository repo, OcrMessagePublisher publisher, MinioFileStorage storage) {
        this.repo = repo;
        this.publisher = publisher;
        this.storage = storage;
        this.documentBucket = storage.getDefaultBucket();
    }

    public Document createDocument(Document doc) {
        if (doc.getUploadDate() == null) {
            doc.setUploadDate(LocalDateTime.now());
        }
        doc.setOcrJobStatus("PENDING");
        
        Document saved = repo.save(doc);
        indexToElastic(saved);
        publishUploaded(saved);
        return saved;
    }

    public Document create(MultipartFile file, String title, String summary, String content) {
        try {
            Document doc = new Document();
            doc.setTitle(title != null ? title : (file.getOriginalFilename() != null ? file.getOriginalFilename() : "Untitled"));
            doc.setSummary(summary);
            doc.setContent(content);
            doc.setUploadDate(LocalDateTime.now());
            doc.setFileName(file.getOriginalFilename());
            doc.setMimeType(file.getContentType());
            doc.setSize(file.getSize());

            String key = UUID.randomUUID() + "-" + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload");
            storage.put(documentBucket, key, file.getBytes(), file.getContentType());
            doc.setStorageBucket(documentBucket);
            doc.setStorageKey(key);

            Document saved = repo.save(doc);
            indexToElastic(saved);
            publishUploaded(saved);
            return saved;
        } catch (Exception e) {
            throw new FileValidationException("Failed to upload to storage: " + e.getMessage(), e);
        }
    }


    public Document replaceFile(int id, MultipartFile file) {
        var doc = repo.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
        try {
            if (doc.getStorageKey() == null) {
                doc.setStorageKey(UUID.randomUUID() + "-" + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload"));
                doc.setStorageBucket(documentBucket);
            }
            storage.put(doc.getStorageBucket(), doc.getStorageKey(), file.getBytes(), file.getContentType());
            doc.setFileName(file.getOriginalFilename());
            doc.setMimeType(file.getContentType());
            doc.setSize(file.getSize());
            Document saved = repo.save(doc);
            indexToElastic(saved);
            publishUploaded(saved);
            return saved;
        } catch (Exception e) {
            throw new FileValidationException("Failed to upload to storage: " + e.getMessage(), e);
        }
    }

    public byte[] getFileBytes(int id) {
        var doc = repo.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
        if (doc.getStorageKey() == null) throw new DocumentNotFoundException(id);
        try {
            return storage.get(doc.getStorageBucket(), doc.getStorageKey());
        } catch (Exception e) {
            throw new FileValidationException("Failed to load from storage", e);
        }
    }

    public void delete(int id) {
        var doc = repo.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
        try {
            if (doc.getStorageKey() != null) {
                storage.delete(doc.getStorageBucket(), doc.getStorageKey());
            }
        } catch (Exception ignore) {}
        repo.deleteById(id);
        // Auch aus Elasticsearch löschen
        if (elasticRepo != null) {
            elasticRepo.deleteById(id);
        }
    }

    public Document partialUpdate(int id, Map<String, Object> updates) {
        Document doc = repo.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));

        if (updates.containsKey("title")) doc.setTitle((String) updates.get("title"));
        if (updates.containsKey("summary")) doc.setSummary((String) updates.get("summary"));
        
        if (updates.containsKey("content")) {
            doc.setContent((String) updates.get("content"));
            doc.setOcrJobStatus("COMPLETED");
        }
        Document saved = repo.save(doc);
        // Elasticsearch-Index aktualisieren
        indexToElastic(saved);
        return saved;
    }

    private void publishUploaded(Document d) {
        publisher.publish(new DocumentUploadedEvent(
                d.getId(), d.getTitle(), d.getFileName(), d.getMimeType(), d.getSize(), d.getUploadDate(),
                d.getStorageBucket(), d.getStorageKey()
        ));
    }

    public Document updateSummary(int id, String summary) {
        Document doc = repo.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));

        // Wenn die Summary bereits gesetzt wurde, darf sie nicht mehr überschrieben werden
        if (SUMMARY_STATUS_DONE.equals(doc.getSummaryStatus())) {
            throw new IllegalStateException("Summary has already been set and cannot be modified.");
        }

        doc.setSummary(summary);
        doc.setSummaryGeneratedAt(LocalDateTime.now());
        doc.setSummaryStatus(SUMMARY_STATUS_DONE);
        Document saved = repo.save(doc);
        // Elasticsearch-Index aktualisieren
        indexToElastic(saved);
        return saved;
    }

    /** Hilfsmethode: Indexiert oder aktualisiert das Dokument in Elasticsearch */
    private void indexToElastic(Document doc) {
        if (elasticRepo != null && doc != null) {
            DocumentIndex idx = new DocumentIndex();
            idx.setId(doc.getId());
            idx.setTitle(doc.getTitle());
            idx.setContent(doc.getContent());
            idx.setSummary(doc.getSummary());
            idx.setSummaryStatus(doc.getSummaryStatus());
            idx.setSummaryGeneratedAt(doc.getSummaryGeneratedAt() != null ? doc.getSummaryGeneratedAt().toString() : null);
            idx.setUploadDate(doc.getUploadDate() != null ? doc.getUploadDate().toString() : null);
            idx.setFileName(doc.getFileName());
            idx.setMimeType(doc.getMimeType());
            idx.setSize(doc.getSize());
            idx.setOcrJobStatus(doc.getOcrJobStatus());
            
            elasticRepo.save(idx);
        }
    }
}