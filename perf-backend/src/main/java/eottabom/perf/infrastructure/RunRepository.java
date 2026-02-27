package eottabom.perf.infrastructure;

import eottabom.perf.domain.Run;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RunRepository extends ListCrudRepository<Run, String> {

	boolean existsByScenarioId(String scenarioId);

	@Query("SELECT * FROM runs ORDER BY started_at DESC, id DESC LIMIT :size")
	List<Run> findFirst(@Param("size") int size);

	@Query("SELECT * FROM runs WHERE started_at < :after OR (started_at = :after AND id < :afterId) ORDER BY started_at DESC, id DESC LIMIT :size")
	List<Run> findAfter(@Param("after") LocalDateTime after, @Param("afterId") String afterId, @Param("size") int size);
}
