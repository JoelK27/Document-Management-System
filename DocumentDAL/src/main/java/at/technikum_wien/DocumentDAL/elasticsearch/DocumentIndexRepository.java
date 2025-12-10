package at.technikum_wien.DocumentDAL.elasticsearch;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface DocumentIndexRepository extends ElasticsearchRepository<DocumentIndex, Integer> {
    @Query("{\"query_string\": {\"query\": \"*?0*\", \"fields\": [\"title^3\", \"summary^2\", \"content\"], \"analyze_wildcard\": true, \"default_operator\": \"AND\"}}")
    List<DocumentIndex> search(String q);
}