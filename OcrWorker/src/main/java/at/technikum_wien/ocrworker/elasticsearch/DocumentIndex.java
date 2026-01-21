package at.technikum_wien.ocrworker.elasticsearch;

import at.technikum_wien.ocrworker.client.BackendClient;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "documents")
@Getter
@Setter
public class DocumentIndex {
    @Id
    private Integer id;
    private String title;
    private String content;
    private String summary;
    private String summaryStatus;
    private String summaryGeneratedAt;
    private String uploadDate;
    private String fileName;
    private String mimeType;
    private Long size;
    private String storageBucket;
    private String storageKey;
    private String previewKey;
    private String ocrJobStatus;

    public DocumentIndex() {}

    // Konstruktor für BackendClient.DocumentDto
    public DocumentIndex(BackendClient.DocumentDto doc) {
        this.id = doc.id;
        this.title = doc.title;
        this.content = doc.content;
        this.summary = doc.summary;
        this.summaryStatus = doc.summaryStatus;
        this.summaryGeneratedAt = doc.summaryGeneratedAt != null ? doc.summaryGeneratedAt : null;
        this.uploadDate = doc.uploadDate != null ? doc.uploadDate : null;
        this.fileName = doc.fileName;
        this.mimeType = doc.mimeType;
        this.size = doc.size;
        this.storageBucket = doc.storageBucket;
        this.storageKey = doc.storageKey;
        this.previewKey = doc.previewKey;
        this.ocrJobStatus = doc.ocrJobStatus;
    }
}