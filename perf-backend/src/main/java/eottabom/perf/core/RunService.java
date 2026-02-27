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

		// executor 검증을 저장 전에 수행해 orphaned Run 방지
		var executor = executors.get(scenario.engine());
		if (executor == null) {
			throw new IllegalArgumentException("Unsupported engine: " + scenario.engine());
		}

		var run = new Run(
				UUID.randomUUID().toString(),
				scenario.id(),
				RunStatus.PENDING,
				LocalDateTime.now(),
				null
		);
		run = runRepository.save(run);

		executor.execute(scenario, run);

		return RunResponse.from(run, scenario);
	}

	public List<RunResponse> findAll(LocalDateTime after, int size) {
		var runs = after == null
				? runRepository.findFirst(size)
				: runRepository.findAfter(after, size);

		// N+1 쿼리 방지: 시나리오를 한 번에 배치 조회
		var scenarioIds = runs.stream().map(Run::scenarioId).collect(Collectors.toSet());
		var scenarioMap = scenarioService.findAllById(scenarioIds).stream()
				.collect(Collectors.toMap(Scenario::id, Function.identity()));

		return runs.stream()
				.map(run -> RunResponse.from(run, scenarioMap.get(run.scenarioId())))
				.toList();
	}

	public RunResponse findById(String id) {
		var run = runRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Run not found: " + id));
		return RunResponse.from(run, scenarioService.findById(run.scenarioId()));
	}

	public void stop(String id) {
		var run = runRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Run not found: " + id));

		if (run.status() != RunStatus.RUNNING && run.status() != RunStatus.PENDING) {
			throw new IllegalStateException("Run is not stoppable: " + run.status().toJson());
		}

		var scenario = scenarioService.findById(run.scenarioId());
		var executor = executors.get(scenario.engine());
		if (executor == null) {
			throw new IllegalArgumentException("Unsupported engine for run: " + id);
		}

		// 상태 업데이트는 executor.stop() 내부에서 처리
		executor.stop(id);
	}
}
