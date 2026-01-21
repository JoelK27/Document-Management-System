package at.technikum_wien.ocrworker.elasticsearch;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataElasticsearchTest
@Disabled("Deaktiviert für CI-Pipeline, da kein live Elasticsearch verfügbar ist.")
class DocumentIndexRepositoryTest {

    @Autowired
    private DocumentIndexRepository repo;

    @Test
    void saveAndFindAllFields() {
        DocumentIndex doc = new DocumentIndex();
        doc.setId(42);
        doc.setTitle("Test Title");
        doc.setContent("Test Content");
        doc.setSummary("Test Summary");
        doc.setSummaryStatus("IN_PROGRESS");
        doc.setSummaryGeneratedAt("2024-01-01T12:00:00");
        doc.setUploadDate("2024-01-01T10:00:00");
        doc.setFileName("test.pdf");
        doc.setMimeType("application/pdf");
        doc.setSize(2048L);
        doc.setStorageBucket("documents");
        doc.setStorageKey("uuid-test.pdf");
        doc.setPreviewKey("preview-uuid-test.pdf");
        doc.setOcrJobStatus("PENDING");

        repo.save(doc);

        Optional<DocumentIndex> found = repo.findById(42);
        assertThat(found).isPresent();
        DocumentIndex loaded = found.get();

        assertThat(loaded.getTitle()).isEqualTo("Test Title");
        assertThat(loaded.getContent()).isEqualTo("Test Content");
        assertThat(loaded.getSummary()).isEqualTo("Test Summary");
        assertThat(loaded.getSummaryStatus()).isEqualTo("IN_PROGRESS");
        assertThat(loaded.getSummaryGeneratedAt()).isEqualTo("2024-01-01T12:00:00");
        assertThat(loaded.getUploadDate()).isEqualTo("2024-01-01T10:00:00");
        assertThat(loaded.getFileName()).isEqualTo("test.pdf");
        assertThat(loaded.getMimeType()).isEqualTo("application/pdf");
        assertThat(loaded.getSize()).isEqualTo(2048L);
        assertThat(loaded.getStorageBucket()).isEqualTo("documents");
        assertThat(loaded.getStorageKey()).isEqualTo("uuid-test.pdf");
        assertThat(loaded.getPreviewKey()).isEqualTo("preview-uuid-test.pdf");
        assertThat(loaded.getOcrJobStatus()).isEqualTo("PENDING");
    }

    @Test
    void findById_ShouldReturnEmptyForMissingDocument() {
        Optional<DocumentIndex> found = repo.findById(9999);
        assertThat(found).isNotPresent();
    }

    @Test
    void saveMultipleAndFindAll() {
        DocumentIndex doc1 = new DocumentIndex();
        doc1.setId(1);
        doc1.setTitle("Doc One");
        repo.save(doc1);

        DocumentIndex doc2 = new DocumentIndex();
        doc2.setId(2);
        doc2.setTitle("Doc Two");
        repo.save(doc2);

        Iterable<DocumentIndex> all = repo.findAll();
        assertThat(all).extracting(DocumentIndex::getId).contains(1, 2);
    }

    @Test
    void deleteById_ShouldRemoveDocument() {
        DocumentIndex doc = new DocumentIndex();
        doc.setId(123);
        doc.setTitle("To be deleted");
        repo.save(doc);

        repo.deleteById(123);
        Optional<DocumentIndex> found = repo.findById(123);
        assertThat(found).isNotPresent();
    }

    @Test
    void save_ShouldOverwriteExistingDocument() {
        DocumentIndex doc = new DocumentIndex();
        doc.setId(77);
        doc.setTitle("Original Title");
        repo.save(doc);

        DocumentIndex updated = new DocumentIndex();
        updated.setId(77);
        updated.setTitle("Updated Title");
        repo.save(updated);

        Optional<DocumentIndex> found = repo.findById(77);
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Updated Title");
    }
}