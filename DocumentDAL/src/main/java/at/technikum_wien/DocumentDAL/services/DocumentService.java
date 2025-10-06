package at.technikum_wien.DocumentDAL.services;

import at.technikum_wien.DocumentDAL.exceptions.DocumentNotFoundException;
import at.technikum_wien.DocumentDAL.exceptions.FileValidationException;
import at.technikum_wien.DocumentDAL.messaging.OcrMessagePublisher;
import at.technikum_wien.DocumentDAL.messaging.events.DocumentUploadedEvent;
import at.technikum_wien.DocumentDAL.model.Document;
import at.technikum_wien.DocumentDAL.repo.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private final DocumentRepository repo;
    private final OcrMessagePublisher publisher;

    public DocumentService(DocumentRepository repo, OcrMessagePublisher publisher) {
        this.repo = repo;
        this.publisher = publisher;
    }

    public Document createDocument(Document doc) {
        if (doc.getUploadDate() == null) {
            doc.setUploadDate(LocalDateTime.now());
        }
        Document saved = repo.save(doc);
        publishUploaded(saved);
        return saved;
    }

    public Document uploadFile(MultipartFile file, String title, String summary, String content) {
        validateFile(file);
        try {
            Document doc = new Document();
            doc.setTitle(title != null ? title : file.getOriginalFilename());
            doc.setSummary(summary);
            doc.setContent(content);
            doc.setUploadDate(LocalDateTime.now());
            doc.setFileName(file.getOriginalFilename());
            doc.setMimeType(file.getContentType());
            doc.setSize(file.getSize());
            doc.setFileData(file.getBytes());
            Document saved = repo.save(doc);
            publishUploaded(saved);
            return saved;
        } catch (Exception e) {
            throw new FileValidationException("Failed to process file: " + e.getMessage());
        }
    }

    public Document replaceFile(int id, MultipartFile file) {
        validateFile(file);
        var existing = repo.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
        try {
            existing.setFileName(file.getOriginalFilename());
            existing.setMimeType(file.getContentType());
            existing.setSize(file.getSize());
            existing.setFileData(file.getBytes());
            Document saved = repo.save(existing);
            publishUploaded(saved);
            return saved;
        } catch (Exception e) {
            throw new FileValidationException("Replace failed: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileValidationException("File exceeds 50MB");
        }
    }

    private void publishUploaded(Document d) {
        publisher.publish(new DocumentUploadedEvent(
                d.getId(),
                d.getTitle(),
                d.getFileName(),
                d.getMimeType(),
                d.getSize(),
                d.getUploadDate()
        ));
    }
}