package at.technikum_wien.ocrworker.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;

@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private final Tesseract tesseract;
    private final int dpi;

    public OcrService(
            // Default zeigt DIREKT auf das tessdata-Verzeichnis
            @Value("${TESSDATA_PREFIX:/usr/share/tesseract-ocr/5/tessdata}") String tessdataDir,
            @Value("${OCR_LANG:deu+eng}") String ocrLang,
            @Value("${OCR_DPI:300}") int dpi
    ) {
        this.dpi = dpi;

        // Falls TESSDATA_PREFIX über Env doch gesetzt wäre: ignorieren wir – wir steuern per setDatapath()
        String envPrefix = System.getenv("TESSDATA_PREFIX");
        if (envPrefix != null && !envPrefix.isBlank()) {
            log.warn("Environment TESSDATA_PREFIX='{}' ist gesetzt, wird aber ignoriert. "
                    + "Wir verwenden setDatapath('{}') explizit.", envPrefix, tessdataDir);
        }

        // Sanity: sicherstellen, dass das Verzeichnis existiert & die Dateien da sind
        File dir = new File(tessdataDir);
        boolean ok = dir.isDirectory()
                && new File(dir, "eng.traineddata").isFile()
                && new File(dir, "deu.traineddata").isFile();

        if (!ok) {
            throw new IllegalStateException("Ungültiges tessdata-Verzeichnis: " + dir.getAbsolutePath()
                    + " (existiert: " + dir.isDirectory() + "). "
                    + "Erwartet: eng.traineddata & deu.traineddata vorhanden.");
        }

        this.tesseract = new Tesseract();
        // setDatapath zeigt HIER auf das tessdata-Verzeichnis selbst:
        this.tesseract.setDatapath(dir.getAbsolutePath());
        this.tesseract.setLanguage(ocrLang);

        log.info("Tesseract init: datapath='{}' | lang='{}' | dpi={}",
                dir.getAbsolutePath(), ocrLang, dpi);
    }

    private boolean isGoodEnough(String text) {
        if (text == null) return false;
        String noWs = text.replaceAll("\\s+", "");
        return text.length() >= 20 && noWs.length() >= (text.length() * 0.5);
    }

    public String extractPreferPdfTextThenOcr(byte[] pdfBytes) throws Exception {
        String pdfText = extractWithPdfBox(pdfBytes).trim();
        if (isGoodEnough(pdfText)) {
            return pdfText;
        }
        return extractWithTesseract(pdfBytes);
    }

    private String extractWithPdfBox(byte[] pdfBytes) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }

    private String extractWithTesseract(byte[] pdfBytes) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            StringBuilder out = new StringBuilder(4096);
            for (int page = 0; page < doc.getNumberOfPages(); page++) {
                BufferedImage img = renderer.renderImageWithDPI(page, dpi, ImageType.RGB);
                String pageText = doOcr(img);
                if (!pageText.isBlank()) {
                    if (!out.isEmpty()) out.append("\n\n--- Page ").append(page + 1).append(" ---\n\n");
                    out.append(pageText.trim());
                }
            }
            return out.toString();
        }
    }

    private String doOcr(BufferedImage img) throws TesseractException {
        return tesseract.doOCR(img);
    }
}
