package at.technikum_wien.ocrworker.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DocumentIndexRepository extends ElasticsearchRepository<DocumentIndex, Integer> {

}