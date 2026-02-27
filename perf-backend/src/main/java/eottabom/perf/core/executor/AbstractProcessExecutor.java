package eottabom.perf.core.executor;

import eottabom.perf.domain.Run;
import eottabom.perf.domain.RunStatus;
import eottabom.perf.domain.Scenario;
import eottabom.perf.infrastructure.RunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

abstract class AbstractProcessExecutor implements TestExecutor {

	private static final Logger logger = LoggerFactory.getLogger(AbstractProcessExecutor.class);

	protected final RunRepository runRepository;
	private final ConcurrentHashMap<String, Process> processes = new ConcurrentHashMap<>();

	protected AbstractProcessExecutor(RunRepository runRepository) {
		this.runRepository = runRepository;
	}

	/**
	 * 실행할 ProcessBuilder를 반환한다. workDir은 실행 완료 후 자동으로 삭제된다.
	 */
	protected abstract ProcessBuilder buildProcess(Scenario scenario, Run run, Path workDir) throws Exception;

	@Override
	public void execute(Scenario scenario, Run run) {
		CompletableFuture.runAsync(() -> {
			var workDir = (Path) null;
			try {
				workDir = Files.createTempDirectory("perf-" + run.id() + "-");
				updateStatus(run, RunStatus.RUNNING);

				var process = buildProcess(scenario, run, workDir)
						.redirectErrorStream(true)
						.start();
				processes.put(run.id(), process);

				// TODO: LogStore에 연결해 SSE로 실시간 스트리밍
				process.getInputStream().transferTo(OutputStream.nullOutputStream());

				var exitCode = process.waitFor();

				// stop()이 먼저 제거한 경우 상태 업데이트 중복 방지
				if (processes.remove(run.id(), process)) {
					var result = exitCode == 0 ? RunStatus.COMPLETED : RunStatus.FAILED;
					updateStatus(run, result);
					logger.info("{} run={} finished exitCode={}", engine(), run.id(), exitCode);
				}
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				updateStatus(run, RunStatus.FAILED);
			} catch (Exception ex) {
				logger.error("{} run={} failed: {}", engine(), run.id(), ex.getMessage(), ex);
				updateStatus(run, RunStatus.FAILED);
			} finally {
				deleteQuietly(workDir);
			}
		});
	}

	@Override
	public void stop(String runId) {
		var process = processes.get(runId);
		if (process != null && processes.remove(runId, process)) {
			process.destroyForcibly();
			logger.info("{} run={} stopped", engine(), runId);
		}
	}

	private void updateStatus(Run run, RunStatus newStatus) {
		var finishedAt = newStatus.isTerminal() ? LocalDateTime.now() : run.finishedAt();
		runRepository.save(new Run(run.id(), run.scenarioId(), newStatus, run.startedAt(), finishedAt));
	}

	private void deleteQuietly(Path path) {
		if (path == null) return;
		try {
			Files.walk(path)
					.sorted(Comparator.reverseOrder())
					.forEach(p -> {
						try {
							Files.delete(p);
						} catch (IOException ignored) {
						}
					});
		} catch (IOException ignored) {
		}
	}
}
