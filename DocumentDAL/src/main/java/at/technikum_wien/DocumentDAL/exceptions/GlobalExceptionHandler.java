package at.technikum_wien.DocumentDAL.exceptions;

import lombok.extern.java.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.http.HttpStatusCode;
import java.io.IOException;
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

    // Leise behandeln, wenn der Client die Verbindung abbricht (z. B. Broken pipe)
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<?> handleAsyncNotUsable(AsyncRequestNotUsableException ex) {
        if (isClientAbort(ex)) {
            log.debug("Client aborted connection: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatusCode.valueOf(499)).build(); // 499: Client Closed Request
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(body("Async request not usable", HttpStatus.SERVICE_UNAVAILABLE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex) {
        if (isClientAbort(ex)) {
            log.debug("Client aborted connection: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatusCode.valueOf(499)).build();
        }
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body("Internal error", HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private boolean isClientAbort(Throwable ex) {
        // Message-basierte Erkennung
        if (containsMessage(ex, "Broken pipe") || containsMessage(ex, "ClientAbortException")) {
            return true;
        }
        // Ursache-Kette prüfen (IOException, Tomcat ClientAbortException ohne direkte Abhängigkeit)
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof IOException && containsMessage(cause, "Broken pipe")) {
                return true;
            }
            if (cause.getClass().getName().equals("org.apache.catalina.connector.ClientAbortException")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private boolean containsMessage(Throwable ex, String needle) {
        String msg = ex.getMessage();
        return msg != null && msg.contains(needle);
    }
}
