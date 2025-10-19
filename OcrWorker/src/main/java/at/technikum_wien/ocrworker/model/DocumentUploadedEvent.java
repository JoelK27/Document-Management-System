package at.technikum_wien.ocrworker.model;

import java.time.LocalDateTime;

public record DocumentUploadedEvent(
        int id,
        String title,
        String fileName,
        String mimeType,
        long size,
        LocalDateTime uploadDate,
        String storageBucket,
        String storageKey
) {}