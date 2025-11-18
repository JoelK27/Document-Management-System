package at.technikum_wien.ocrworker.model;

public record DocumentOcrCompletedEvent(
        int id,
        String storageBucket,
        String storageKey,
        String extractedText
) {}