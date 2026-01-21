package at.technikum_wien.accesslog.controller;

import at.technikum_wien.accesslog.service.LogImporterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class ImportController {

    private final LogImporterService importerService;

    // Aufrufen via POST http://localhost:8082/api/batch/trigger
    @PostMapping("/trigger")
    public ResponseEntity<String> triggerImport() {
        importerService.importAccessLogs();
        return ResponseEntity.ok("Batch import triggered manually.");
    }
}