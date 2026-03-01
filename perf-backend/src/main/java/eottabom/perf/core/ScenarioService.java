package eottabom.perf.core;

import eottabom.perf.api.dto.CreateScenarioRequest;
import eottabom.perf.api.dto.UpdateScenarioRequest;
import eottabom.perf.domain.Scenario;
import eottabom.perf.infrastructure.RunRepository;
import eottabom.perf.infrastructure.ScenarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ScenarioService {

	private final ScenarioRepository scenarioRepository;
	private final RunRepository runRepository;

	public ScenarioService(ScenarioRepository scenarioRepository,
						   RunRepository runRepository) {
		this.scenarioRepository = scenarioRepository;
		this.runRepository = runRepository;
	}

	public List<Scenario> findAll(LocalDateTime after, String afterId, int size) {
		return after == null
				? scenarioRepository.findFirst(size)
				: scenarioRepository.findAfter(after, normalizeCursorId(afterId), size);
	}

	public List<Scenario> findAllById(Collection<String> ids) {
		return scenarioRepository.findAllById(ids);
	}

	public Scenario findById(String id) {
		return scenarioRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Scenario not found: " + id));
	}

	public Scenario create(CreateScenarioRequest request) {
		var scenario = new Scenario(
				UUID.randomUUID().toString(),
				request.name(),
				request.engine(),
				request.content(),
				request.description(),
				LocalDateTime.now(),
				null
		);
		return scenarioRepository.save(scenario);
	}

	public Scenario update(String id, UpdateScenarioRequest request) {
		var existing = findById(id);
		var updated = new Scenario(
				existing.id(),
				request.name() != null ? request.name() : existing.name(),
				existing.engine(),
				request.content() != null ? request.content() : existing.content(),
				request.description() != null ? request.description() : existing.description(),
				existing.createdAt(),
				LocalDateTime.now()
		);
		return scenarioRepository.save(updated);
	}

	public void delete(String id) {
		if (!scenarioRepository.existsById(id)) {
			throw new NoSuchElementException("Scenario not found: " + id);
		}
		if (runRepository.existsByScenarioId(id)) {
			throw new IllegalStateException("Scenario has runs and cannot be deleted: " + id);
		}
		scenarioRepository.deleteById(id);
	}

	private static String normalizeCursorId(String afterId) {
		return (afterId == null || afterId.isBlank()) ? "~" : afterId;
	}
}
