package at.technikum_wien.DocumentDAL.controller;

import at.technikum_wien.DocumentDAL.model.Document;
import at.technikum_wien.DocumentDAL.repo.DocumentRepository;
import at.technikum_wien.DocumentDAL.services.PdfPreviewService;
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
    private final PdfPreviewService pdfPreviewService = new PdfPreviewService();

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

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("application/pdf", "text/plain");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "txt");

    private boolean isAllowed(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        String ct = file.getContentType();
        String original = file.getOriginalFilename();
        if (original == null) return false;
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        // Manche Browser liefern z.B. bei .txt manchmal "text/plain", bei pdf "application/pdf"
        return ALLOWED_EXTENSIONS.contains(ext) && (ct == null || ALLOWED_CONTENT_TYPES.contains(ct));
    }

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50 MB

    // Multipart Upload: Datei + optionale Metadaten
    @PostMapping(path = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "summary", required = false) String summary,
            @RequestParam(value = "content", required = false) String content
    ) throws Exception {

        if (file.isEmpty()){
            return ResponseEntity.badRequest()
                    .body("The file can't be empty.");
        }

        if (!isAllowed(file)) {
            return ResponseEntity.badRequest()
                    .body("Only PDF and TXT files are allowed.");
        }

        if(file.getBytes().length > MAX_FILE_SIZE){
            return ResponseEntity.badRequest()
                    .body("The file to be uploaded can't exceed 50MB");
        }

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
