package at.technikum_wien.DocumentDAL.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface DocumentIndexRepository extends ElasticsearchRepository<DocumentIndex, Integer> {
    List<DocumentIndex> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(String title, String content);
}