package at.technikum_wien.DocumentDAL.controller;

import at.technikum_wien.DocumentDAL.model.Document;
import at.technikum_wien.DocumentDAL.repo.DocumentRepository;
import at.technikum_wien.DocumentDAL.services.DocumentService;
import at.technikum_wien.DocumentDAL.services.PdfPreviewService;
import at.technikum_wien.DocumentDAL.validation.AllowedMime;
import at.technikum_wien.DocumentDAL.validation.MaxFileSize;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentRepository repo;
    private final DocumentService documentService;
    private final PdfPreviewService pdfPreviewService = new PdfPreviewService();
    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    public DocumentController(DocumentRepository repo, DocumentService documentService) {
        this.repo = repo;
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Document> upload(@RequestBody Document doc) {
        log.info("Uploading JSON document title='{}'", doc.getTitle());
        Document saved = documentService.createDocument(doc);
        return ResponseEntity.created(URI.create("/api/documents/" + saved.getId())).body(saved);
    }

    @PostMapping(path = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @Valid
            @AllowedMime(types = { "application/pdf", "text/plain" })
            @MaxFileSize(bytes = MAX_FILE_SIZE)
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "summary", required = false) String summary,
            @RequestParam(value = "content", required = false) String content
    ) {
        log.info("Uploading file '{}'", file != null ? file.getOriginalFilename() : "null");
        Document saved = documentService.uploadFile(file, title, summary, content);
        return ResponseEntity.created(URI.create("/api/documents/" + saved.getId())).body(saved);
    }

    @PutMapping(path = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> replaceFile(
            @PathVariable int id,
            @Valid
            @AllowedMime(types = { "application/pdf", "text/plain" })
            @MaxFileSize(bytes = MAX_FILE_SIZE)
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Replacing file for document id={}", id);
        Document saved = documentService.replaceFile(id, file);
        return ResponseEntity.ok(saved);
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

    @GetMapping("/{id}/preview")
    public ResponseEntity<?> preview(@PathVariable int id) {
        return repo.findById(id).map(doc -> {
            if (doc.getMimeType() == null ||
                    !doc.getMimeType().equalsIgnoreCase("application/pdf") ||
                    doc.getFileData() == null) {
                return ResponseEntity.notFound().build();
            }
            try {
                byte[] png = pdfPreviewService.renderFirstPageAsPng(doc.getFileData());
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .contentType(MediaType.IMAGE_PNG)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"preview-" + doc.getId() + ".png\"")
                        .body(png);
            } catch (IOException e) {
                return ResponseEntity.internalServerError()
                        .body(null);
            }
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
