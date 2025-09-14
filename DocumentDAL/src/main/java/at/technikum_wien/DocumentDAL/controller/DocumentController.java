package at.technikum_wien.DocumentDAL.controller;

import at.technikum_wien.DocumentDAL.model.Document;
import at.technikum_wien.DocumentDAL.repo.DocumentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository repo;

    public DocumentController(DocumentRepository repo) {
        this.repo = repo;
    }

    // POST /api/documents/upload
    @PostMapping("/upload")
    public ResponseEntity<Document> upload(@RequestBody Document doc) {
        if (doc.getUploadDate() == null) {
            doc.setUploadDate(LocalDateTime.now());
        }
        Document saved = repo.save(doc);
        return ResponseEntity.created(URI.create("/api/documents/" + saved.getId())).body(saved);
    }

    // GET /api/documents/search?q={query}
    @GetMapping("/search")
    public ResponseEntity<List<Document>> search(@RequestParam("q") String q) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(repo.findAll());
        }
        List<Document> results = repo
                .search(q);
        return ResponseEntity.ok(results);
    }

    // PUT /api/documents/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Document> update(@PathVariable int id, @RequestBody Document incoming) {
        return repo.findById(id)
                .map(existing -> {
                    // Vollständiges Update (uploadDate beibehalten falls null übergeben)
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
