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
