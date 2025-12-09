package at.technikum_wien.ocrworker.elasticsearch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataElasticsearchTest
class DocumentIndexRepositoryTest {

    @Autowired
    private DocumentIndexRepository repo;

    @Test
    void saveAndFindById() {
        DocumentIndex doc = new DocumentIndex(1, "Test", "Hello World", "test.pdf");
        repo.save(doc);

        var found = repo.findById(1);
        assertThat(found).isPresent();
        assertThat(found.get().getContent()).contains("Hello");
    }
}