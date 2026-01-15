package at.technikum_wien.accesslog.repo;

import at.technikum_wien.accesslog.model.AccessStat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface AccessStatRepository extends JpaRepository<AccessStat, Integer> {
    Optional<AccessStat> findByDocumentIdAndAccessDate(Integer documentId, LocalDate accessDate);
}