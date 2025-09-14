package at.technikum_wien.DocumentDAL.repo;

import at.technikum_wien.DocumentDAL.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Integer> {

    @Query("select d from Document d " +
            "where lower(d.title) like lower(concat('%', :q, '%')) " +
            "   or lower(d.content) like lower(concat('%', :q, '%')) " +
            "   or lower(d.summary) like lower(concat('%', :q, '%'))")
    List<Document> search(@Param("q") String q);
}
