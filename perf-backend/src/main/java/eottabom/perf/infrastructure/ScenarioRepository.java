package eottabom.perf.infrastructure;

import eottabom.perf.domain.Scenario;
import org.springframework.data.repository.CrudRepository;

public interface ScenarioRepository extends CrudRepository<Scenario, String> {
}
