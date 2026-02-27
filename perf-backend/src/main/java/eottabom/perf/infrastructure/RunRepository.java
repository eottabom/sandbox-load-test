package eottabom.perf.infrastructure;

import eottabom.perf.domain.Run;
import org.springframework.data.repository.CrudRepository;

public interface RunRepository extends CrudRepository<Run, String> {
	boolean existsByScenarioId(String scenarioId);
}
