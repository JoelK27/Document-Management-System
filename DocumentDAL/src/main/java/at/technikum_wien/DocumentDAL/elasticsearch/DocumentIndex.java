package at.technikum_wien.DocumentDAL.elasticsearch;

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
}
