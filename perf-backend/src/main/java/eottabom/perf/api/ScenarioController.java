package eottabom.perf.api;

import eottabom.perf.api.dto.CreateScenarioRequest;
import eottabom.perf.api.dto.ScenarioDetailResponse;
import eottabom.perf.api.dto.ScenarioResponse;
import eottabom.perf.api.dto.UpdateScenarioRequest;
import eottabom.perf.core.ScenarioService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

	private final ScenarioService scenarioService;

	public ScenarioController(ScenarioService scenarioService) {
		this.scenarioService = scenarioService;
	}

	@GetMapping
	public List<ScenarioResponse> list(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after,
			@RequestParam(required = false) String afterId,
			@RequestParam(defaultValue = "50") int size) {
		return scenarioService.findAll(after, afterId, Math.clamp(size, 1, 200)).stream()
				.map(ScenarioResponse::from)
				.toList();
	}

	@GetMapping("/{id}")
	public ScenarioDetailResponse get(@PathVariable String id) {
		return ScenarioDetailResponse.from(scenarioService.findById(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ScenarioDetailResponse create(@Valid @RequestBody CreateScenarioRequest request) {
		return ScenarioDetailResponse.from(scenarioService.create(request));
	}

	@PutMapping("/{id}")
	public ScenarioDetailResponse update(@PathVariable String id,
										 @Valid @RequestBody UpdateScenarioRequest request) {
		return ScenarioDetailResponse.from(scenarioService.update(id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable String id) {
		scenarioService.delete(id);
	}
}
