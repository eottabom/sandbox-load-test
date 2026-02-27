package eottabom.perf.infrastructure;

import eottabom.perf.domain.Scenario;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScenarioRepository extends ListCrudRepository<Scenario, String> {

	@Query("SELECT * FROM scenarios ORDER BY created_at DESC LIMIT :size")
	List<Scenario> findFirst(@Param("size") int size);

	@Query("SELECT * FROM scenarios WHERE created_at < :after ORDER BY created_at DESC LIMIT :size")
	List<Scenario> findAfter(@Param("after") LocalDateTime after, @Param("size") int size);
}
