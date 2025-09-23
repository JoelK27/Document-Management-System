package at.technikum_wien.DocumentDAL.controller;

import at.technikum_wien.DocumentDAL.model.Document;
import at.technikum_wien.DocumentDAL.repo.DocumentRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentRepository repo;

    public DocumentController(DocumentRepository repo) {
        this.repo = repo;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity.badRequest().body("Error: " + e.getMessage());
    }

    // JSON-basierter Upload (Textfelder)
    @PostMapping("/upload")
    public ResponseEntity<Document> upload(@RequestBody Document doc) {
        if (doc.getUploadDate() == null) {
            doc.setUploadDate(LocalDateTime.now());
        }
        Document saved = repo.save(doc);
        return ResponseEntity.created(URI.create("/api/documents/" + saved.getId())).body(saved);
    }

    // Multipart Upload: Datei + optionale Metadaten
    @PostMapping(path = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Document> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "summary", required = false) String summary,
            @RequestParam(value = "content", required = false) String content
    ) throws Exception {
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
        return ResponseEntity.created(URI.create("/api/documents/" + saved.getId())).body(saved);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> downloadFile(@PathVariable int id) {
        var opt = repo.findById(id);
        if (opt.isEmpty() || opt.get().getFileData() == null) {
            return ResponseEntity.notFound().build();
        }
        var doc = opt.get();
        String fileName = doc.getFileName() != null ? doc.getFileName() : ("document-" + id);
        String contentType = doc.getMimeType() != null ? doc.getMimeType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String contentDisposition = "attachment; filename*=UTF-8''" +
                java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(doc.getSize()))
                .body(doc.getFileData());
    }

    // Datei ersetzen
    @PutMapping(path = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Document> replaceFile(@PathVariable int id, @RequestParam("file") MultipartFile file) throws Exception {
        var opt = repo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        var existing = opt.get();
        existing.setFileName(file.getOriginalFilename());
        existing.setMimeType(file.getContentType());
        existing.setSize(file.getSize());
        existing.setFileData(file.getBytes());
        Document saved = repo.save(existing);
        return ResponseEntity.ok(saved);
    }

    // Suche (q optional)
    @GetMapping("/search")
    public ResponseEntity<List<Document>> search(@RequestParam(value = "q", required = false, defaultValue = "") String q) {
        if (q == null ||q.isBlank()) {
            return ResponseEntity.ok(repo.findAllWithoutFileData());
        }
        List<Document> results = repo.searchWithoutFileData(q);
        return ResponseEntity.ok(results);
    }

    // GET /api/documents/
    @GetMapping
    public ResponseEntity<List<Document>> getAll() {
        return ResponseEntity.ok(repo.findAll());
    }

    // GET /api/documents/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Document> getById(@PathVariable int id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // PUT /api/documents/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Document> update(@PathVariable int id, @RequestBody Document incoming) {
        return repo.findById(id)
                .map(existing -> {
                    existing.setTitle(incoming.getTitle());
                    existing.setContent(incoming.getContent());
                    existing.setSummary(incoming.getSummary());
                    if (incoming.getUploadDate() != null) {
                        existing.setUploadDate(incoming.getUploadDate());
                    }
                    Document saved = repo.save(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // DELETE /api/documents/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
