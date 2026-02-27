package eottabom.perf.infrastructure;

import eottabom.perf.domain.Run;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RunRepository extends ListCrudRepository<Run, String> {

	boolean existsByScenarioId(String scenarioId);

	@Query("SELECT * FROM runs ORDER BY started_at DESC LIMIT :size")
	List<Run> findFirst(@Param("size") int size);

	@Query("SELECT * FROM runs WHERE started_at < :after ORDER BY started_at DESC LIMIT :size")
	List<Run> findAfter(@Param("after") LocalDateTime after, @Param("size") int size);
}
