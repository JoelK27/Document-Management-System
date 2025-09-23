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

    public Document(int id, String title, String content, String summary, LocalDateTime uploadDate,
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

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String title;
    private String content;
    private String summary;
    private LocalDateTime uploadDate;

    private String fileName;
    private String mimeType;
    private long size;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore // nicht in JSON-Listen/Detail mitschicken
    private byte[] fileData;
}
