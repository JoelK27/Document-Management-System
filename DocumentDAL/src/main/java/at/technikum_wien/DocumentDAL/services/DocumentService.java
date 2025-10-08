package at.technikum_wien.DocumentDAL.services;

import at.technikum_wien.DocumentDAL.exceptions.DocumentNotFoundException;
import at.technikum_wien.DocumentDAL.exceptions.FileValidationException;
import at.technikum_wien.DocumentDAL.messaging.OcrMessagePublisher;
import at.technikum_wien.DocumentDAL.messaging.events.DocumentUploadedEvent;
import at.technikum_wien.DocumentDAL.model.Document;
import at.technikum_wien.DocumentDAL.repo.DocumentRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class DocumentService {

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
        try {
            Document doc = new Document();
            doc.setTitle(title != null ? title : file.getOriginalFilename());
            doc.setSummary(summary);
            doc.setContent(content);
            doc.setUploadDate(LocalDateTime.now());
            return setDoc(file, doc);
        } catch (Exception e) {
            throw new FileValidationException("Failed to process file: " + e.getMessage());
        }
    }

    @NotNull
    private Document setDoc(MultipartFile file, Document doc) throws IOException {
        doc.setFileName(file.getOriginalFilename());
        doc.setMimeType(file.getContentType());
        doc.setSize(file.getSize());
        doc.setFileData(file.getBytes());
        Document saved = repo.save(doc);
        publishUploaded(saved);
        return saved;
    }

    public Document replaceFile(int id, MultipartFile file) {
        var existing = repo.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
        try {
            return setDoc(file, existing);
        } catch (Exception e) {
            throw new FileValidationException("Replace failed: " + e.getMessage());
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