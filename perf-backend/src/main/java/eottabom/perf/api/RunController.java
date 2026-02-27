package eottabom.perf.api;

import eottabom.perf.api.dto.RunResponse;
import eottabom.perf.api.dto.StartRunRequest;
import eottabom.perf.core.RunService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/runs")
public class RunController {

	private final RunService runService;

	public RunController(RunService runService) {
		this.runService = runService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RunResponse start(@Valid @RequestBody StartRunRequest request) {
		return runService.start(request);
	}

	@GetMapping
	public List<RunResponse> list(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after,
			@RequestParam(required = false) String afterId,
			@RequestParam(defaultValue = "50") int size) {
		return runService.findAll(after, afterId, Math.clamp(size, 1, 200));
	}

	@GetMapping("/{id}")
	public RunResponse get(@PathVariable String id) {
		return runService.findById(id);
	}

	@PostMapping("/{id}/stop")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void stop(@PathVariable String id) {
		runService.stop(id);
	}

	// TODO: GET /{id}/report
	// TODO: POST /{id}/report/ai
	// TODO: GET /{id}/logs (SSE)
}
