package at.technikum_wien.ocrworker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OcrServiceTest {

    private OcrService ocrService;

    // JUnit erstellt einen Temp-Ordner, der nach dem Test gelöscht wird
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // 1. Wir "faken" eine Tesseract-Installation
        // Der OcrService Konstruktor prüft: if (!new File(dir, "eng.traineddata").isFile()) ...
        File tessDataDir = tempDir.toFile();
        new File(tessDataDir, "eng.traineddata").createNewFile();
        new File(tessDataDir, "deu.traineddata").createNewFile();

        // 2. Manuelle Instanziierung mit Pfad zum Temp-Ordner
        // Dadurch besteht der Service seine eigenen Checks im Konstruktor
        // Hinweis: new Tesseract() wird aufgerufen, lädt aber Bibliotheken meist erst bei Benutzung (doOcr)
        ocrService = new OcrService(tessDataDir.getAbsolutePath(), "eng+deu", 300);
    }

    @Test
    void isGoodEnough_ShouldReturnTrue_ForValidText() {
        // Text mit > 20 Zeichen und > 50% "Nicht-Whitespace"
        String validText = "Dies ist ein vernünftiger Text aus einem PDF extrahiert.";

        Boolean result = (Boolean) ReflectionTestUtils.invokeMethod(ocrService, "isGoodEnough", validText);

        assertThat(result).isTrue();
    }

    @Test
    void isGoodEnough_ShouldReturnFalse_ForGarbageText() {
        // Zu kurz (< 20)
        String shortText = "Hi";
        Boolean resultShort = (Boolean) ReflectionTestUtils.invokeMethod(ocrService, "isGoodEnough", shortText);
        assertThat(resultShort).isFalse();

        // Zu viele Leerzeichen ("Spaced Text" durch schlechtes OCR/PDF-Extract)
        // "h e l l o" -> 5 chars Text, 4 Spaces. Verhältnis ok.
        // Aber: "text      "
        String garbage = "   .   .   "; // fast nur Whitespace
        Boolean resultGarbage = (Boolean) ReflectionTestUtils.invokeMethod(ocrService, "isGoodEnough", garbage);
        assertThat(resultGarbage).isFalse();
    }
}