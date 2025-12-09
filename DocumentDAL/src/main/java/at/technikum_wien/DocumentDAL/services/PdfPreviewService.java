package at.technikum_wien.DocumentDAL.services;

import at.technikum_wien.DocumentDAL.messaging.OcrMessagePublisher;
import at.technikum_wien.DocumentDAL.model.Document;
import at.technikum_wien.DocumentDAL.repo.DocumentRepository;
import at.technikum_wien.DocumentDAL.storage.MinioFileStorage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
@Service
public class PdfPreviewService {

    private final MinioFileStorage storage;
    private final DocumentRepository repo;
    private final String previewBucket;

    public PdfPreviewService(MinioFileStorage storage, DocumentRepository repo) {
        this.storage = storage;
        this.repo = repo;
        this.previewBucket = storage.getPreviewBucket();
    }

    public byte[] renderFirstPageAsPng(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 150);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(image, "png", baos);
                return baos.toByteArray();
            }
        }
    }

    public byte[] getPreview(Document doc, byte[] pdfBytes) throws Exception {
        if(doc.getPreviewKey() != null){
            return storage.get(previewBucket, doc.getPreviewKey());
        }
        byte[] pngPreview = renderFirstPageAsPng(pdfBytes);
        savePreviewToBucket(pngPreview, doc);

        return pngPreview;
    }

    public void savePreviewToBucket(byte[] pngBytes, Document doc) throws Exception {
        String previewKey = doc.getStorageKey().replaceAll("\\.pdf$", ".png");
        storage.put(previewBucket, previewKey, pngBytes, "image/png");
        doc.setPreviewKey(previewKey);
        repo.save(doc);
    }
}


