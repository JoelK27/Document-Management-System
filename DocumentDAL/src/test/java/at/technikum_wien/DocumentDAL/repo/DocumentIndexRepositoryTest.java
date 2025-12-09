package at.technikum_wien.DocumentDAL.repo;

import at.technikum_wien.DocumentDAL.elasticsearch.DocumentIndex;
import at.technikum_wien.DocumentDAL.elasticsearch.DocumentIndexRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataElasticsearchTest
class DocumentIndexRepositoryTest {

    @Autowired
    private DocumentIndexRepository repo;

    @Test
    void saveAndSearchDocument() {
        DocumentIndex doc = new DocumentIndex();
        doc.setId(1);
        doc.setTitle("HelloWorld");
        doc.setContent("Hello from Elasticsearch");
        doc.setFileName("HelloWorld.pdf");
        repo.save(doc);

        List<DocumentIndex> found = repo.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase("hello", "hello");
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).getFileName()).isEqualTo("HelloWorld.pdf");
    }
}