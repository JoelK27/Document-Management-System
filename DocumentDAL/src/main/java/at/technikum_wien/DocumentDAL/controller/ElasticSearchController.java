package at.technikum_wien.DocumentDAL.controller;

import at.technikum_wien.DocumentDAL.elasticsearch.DocumentIndex;
import at.technikum_wien.DocumentDAL.elasticsearch.DocumentIndexRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/elastic")
public class ElasticSearchController {

    private final DocumentIndexRepository repo;

    public ElasticSearchController(DocumentIndexRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/search")
    public List<DocumentIndex> search(@RequestParam String q) {
        return repo.search(q);
    }
}