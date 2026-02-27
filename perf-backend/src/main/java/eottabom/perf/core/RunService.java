package eottabom.perf.core;

import eottabom.perf.api.dto.RunResponse;
import eottabom.perf.api.dto.StartRunRequest;
import eottabom.perf.core.executor.TestExecutor;
import eottabom.perf.domain.Engine;
import eottabom.perf.domain.Run;
import eottabom.perf.domain.RunStatus;
import eottabom.perf.domain.Scenario;
import eottabom.perf.infrastructure.RunRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RunService {

	private final RunRepository runRepository;
	private final ScenarioService scenarioService;
	private final Map<Engine, TestExecutor> executors;

	public RunService(RunRepository runRepository,
					  ScenarioService scenarioService,
					  List<TestExecutor> executors) {
		this.runRepository = runRepository;
		this.scenarioService = scenarioService;
		this.executors = executors.stream()
				.collect(Collectors.toMap(TestExecutor::engine, Function.identity()));
	}

	public RunResponse start(StartRunRequest request) {
		var scenario = scenarioService.findById(request.scenarioId());

		var run = new Run(
				UUID.randomUUID().toString(),
				scenario.id(),
				RunStatus.PENDING,
				LocalDateTime.now(),
				null
		);
		run = runRepository.save(run);

		var executor = executors.get(scenario.engine());
		if (executor == null) {
			throw new IllegalArgumentException("Unsupported engine: " + scenario.engine());
		}
		executor.execute(scenario, run);

		return RunResponse.from(run, scenario);
	}

	public List<RunResponse> findAll() {
		return ((List<Run>) runRepository.findAll()).stream()
				.map(run -> RunResponse.from(run, getScenarioForRun(run)))
				.toList();
	}

	public RunResponse findById(String id) {
		var run = runRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Run not found: " + id));
		return RunResponse.from(run, getScenarioForRun(run));
	}

	public void stop(String id) {
		var run = runRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Run not found: " + id));

		if (run.status() != RunStatus.RUNNING && run.status() != RunStatus.PENDING) {
			throw new IllegalStateException("Run is not stoppable: " + run.status());
		}

		var executor = executors.get(getScenarioForRun(run).engine());
		if (executor == null) {
			throw new IllegalArgumentException("Unsupported engine for run: " + id);
		}
		executor.stop(id);

		var stopped = new Run(run.id(), run.scenarioId(), RunStatus.STOPPED, run.startedAt(), LocalDateTime.now());
		runRepository.save(stopped);
	}

	private Scenario getScenarioForRun(Run run) {
		try {
			return scenarioService.findById(run.scenarioId());
		} catch (NoSuchElementException ex) {
			throw new NoSuchElementException("Scenario not found for run: " + run.id());
		}
	}
}
