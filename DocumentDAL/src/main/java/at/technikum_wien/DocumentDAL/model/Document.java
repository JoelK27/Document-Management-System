package at.technikum_wien.DocumentDAL.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Document {

    public Document() {}

    public Document(Integer id, String title, String content, String summary, LocalDateTime uploadDate,
                    String fileName, String mimeType, long size) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.summary = summary;
        this.uploadDate = uploadDate;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.size = size;
    }

    public Document(Integer id, String title, String summary, LocalDateTime uploadDate,
                    String fileName, String mimeType, long size) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.uploadDate = uploadDate;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.size = size;
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Column(columnDefinition = "text")
    private String summary;

    private LocalDateTime uploadDate;

    private String fileName;
    private String mimeType;
    private long size;
    private String storageBucket;
    private String storageKey;
    private String previewKey;
}
