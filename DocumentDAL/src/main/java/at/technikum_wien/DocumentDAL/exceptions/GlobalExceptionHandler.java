package at.technikum_wien.DocumentDAL.exceptions;

import lombok.extern.java.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private Map<String, Object> body(String message, HttpStatus status) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        );
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<?> handleNotFound(DocumentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(ex.getMessage(), HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<?> handleValidation(FileValidationException ex) {
        return ResponseEntity.badRequest().body(body(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(PreviewGenerationException.class)
    public ResponseEntity<?> handlePreview(PreviewGenerationException ex) {
        log.error("Preview generation failed: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body("Internal error", HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
