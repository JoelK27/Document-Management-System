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
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentRepository repo;
    private final DocumentService service;
    private final PdfPreviewService pdfPreviewService;
    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    public DocumentController(DocumentRepository repo, DocumentService documentService,  PdfPreviewService pdfPreviewService) {
        this.repo = repo;
        this.service = documentService;
        this.pdfPreviewService = pdfPreviewService;
    }

    // JSON-Metadaten speichern (ohne Datei)
    @PostMapping("/upload")
    public ResponseEntity<Document> upload(@RequestBody Document doc) {
        log.info("Uploading JSON document title='{}'", doc.getTitle());
        Document saved = service.createDocument(doc);
        return ResponseEntity.created(URI.create("/api/documents/" + saved.getId())).body(saved);
    }

    // Datei hochladen (Multipart) -> MinIO
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
        Document saved = service.create(file, title, summary, content);
        return ResponseEntity.created(URI.create("/api/documents/" + saved.getId())).body(saved);
    }

    // Datei ersetzen -> MinIO
    @PutMapping(path = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> replaceFile(
            @PathVariable int id,
            @Valid
            @AllowedMime(types = { "application/pdf", "text/plain" })
            @MaxFileSize(bytes = MAX_FILE_SIZE)
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Replacing file for document id={}", id);
        Document saved = service.replaceFile(id, file);
        return ResponseEntity.ok(saved);
    }

    // Datei herunterladen aus MinIO
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> downloadFile(@PathVariable int id) {
        var doc = repo.findById(id).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] data = service.getFileBytes(id);

        String fileName = doc.getFileName() != null ? doc.getFileName() : ("document-" + id);
        String contentType = doc.getMimeType() != null ? doc.getMimeType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String contentDisposition = "attachment; filename*=UTF-8''" +
                java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(data.length))
                .body(data);
    }

    // Suche (q optional)
    @GetMapping("/search")
    public ResponseEntity<List<Document>> search(@RequestParam(value = "q", required = false, defaultValue = "") String q) {
        if (q == null || q.isBlank()) {
            // Falls findAllWithoutFileData existiert, weiterverwenden; bei MinIO gibt es kein fileData mehr.
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

    // DELETE /api/documents/{id} (MinIO + DB)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        service.delete(id); // entfernt Objekt in MinIO und Datensatz in DB
        return ResponseEntity.noContent().build();
    }

    // PDF-Preview (erste Seite als PNG) aus MinIO
    @GetMapping("/{id}/preview")
    public ResponseEntity<?> preview(@PathVariable int id) {
        return repo.findById(id).map(doc -> {
            if (doc.getMimeType() == null || !doc.getMimeType().equalsIgnoreCase("application/pdf")) {
                return ResponseEntity.notFound().build();
            }
            try {
                byte[] src = service.getFileBytes(id);
                byte[] png = pdfPreviewService.getPreview(doc, src);
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .contentType(MediaType.IMAGE_PNG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview-" + doc.getId() + ".png\"")
                        .body(png);
            } catch (IOException e) {
                log.error("Preview rendering failed for id={}", id, e);
                return ResponseEntity.internalServerError().body(null);
            } catch (Exception e) {
                log.error("Storage load failed for id={}", id, e);
                return ResponseEntity.internalServerError().body(null);
            }
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}