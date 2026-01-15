package at.technikum_wien.DocumentDAL.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "document_access_stats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"document_id", "access_date"})
})
@Getter
@Setter
public class DocumentAccessStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Integer documentId;

    @Column(name = "access_date", nullable = false)
    private LocalDate accessDate;

    @Column(name = "access_count", nullable = false)
    private Integer accessCount;

    public DocumentAccessStat() {}

    public DocumentAccessStat(Integer documentId, LocalDate accessDate, Integer accessCount) {
        this.documentId = documentId;
        this.accessDate = accessDate;
        this.accessCount = accessCount;
    }
}