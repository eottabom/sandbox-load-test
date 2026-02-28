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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

abstract class AbstractProcessExecutor implements TestExecutor {

	private static final Logger logger = LoggerFactory.getLogger(AbstractProcessExecutor.class);
	private static final long MAX_RUN_SECONDS = 60 * 60;

	private static final ExecutorService IO_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

	protected final RunRepository runRepository;
	private final ConcurrentHashMap<String, Process> processes = new ConcurrentHashMap<>();
	private final Set<String> cancelledRuns = ConcurrentHashMap.newKeySet();

	protected AbstractProcessExecutor(RunRepository runRepository) {
		this.runRepository = runRepository;
	}

	/**
	 * 실행할 ProcessBuilder를 반환한다. workDir은 실행 완료 후 자동으로 삭제된다.
	 */
	protected abstract ProcessBuilder buildProcess(Scenario scenario, Run run, Path workDir) throws Exception;

	@Override
	public void execute(Scenario scenario, Run run) {
		IO_EXECUTOR.submit(() -> runInBackground(scenario, run));
	}

	private void runInBackground(Scenario scenario, Run run) {
		Path workDir = null;
		Process process = null;
		try {
			workDir = Files.createTempDirectory("perf-" + run.id() + "-");

			// PENDING 상태에서 stop()이 먼저 호출된 경우
			if (cancelledRuns.remove(run.id())) {
				return;
			}

			ExecutorSupport.updateStatus(runRepository, run, RunStatus.RUNNING);

			process = buildProcess(scenario, run, workDir).redirectErrorStream(true).start();
			processes.put(run.id(), process);

			// put 직후 stop()이 끼어든 경우 — destroyForcibly()는 이미 종료된 프로세스에도 안전
			if (cancelledRuns.remove(run.id())) {
				process.destroyForcibly();
				ExecutorSupport.updateStatus(runRepository, run, RunStatus.STOPPED);
				return;
			}

			// TODO: LogStore에 연결해 SSE로 실시간 스트리밍
			drainOutputAsync(process);

			var completed = process.waitFor(MAX_RUN_SECONDS, TimeUnit.SECONDS);
			if (!completed) {
				handleTimeout(run, process);
				return;
			}

			// 완료 후 소유권 확인 — stop()이 먼저 map에서 제거한 경우 skip
			var exitCode = process.exitValue();
			var ownedByUs = processes.remove(run.id(), process);
			if (!ownedByUs) {
				return;
			}

			var cancelledAtFinish = cancelledRuns.remove(run.id());
			if (!cancelledAtFinish) {
				ExecutorSupport.updateStatus(runRepository, run, ExecutorSupport.toRunStatus(exitCode));
				logger.info("{} run={} finished exitCode={}", engine(), run.id(), exitCode);
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			markFailedIfNotCancelled(run);
		} catch (Exception ex) {
			logger.error("{} run={} failed: {}", engine(), run.id(), ex.getMessage(), ex);
			markFailedIfNotCancelled(run);
		} finally {
			if (process != null) {
				processes.remove(run.id(), process);
			}
			ExecutorSupport.deleteQuietly(workDir);
		}
	}

	private void drainOutputAsync(Process process) {
		IO_EXECUTOR.submit(() -> {
			try {
				process.getInputStream().transferTo(OutputStream.nullOutputStream());
			} catch (IOException ignored) {
				// 프로세스 종료 시 스트림이 닫히면 자연스럽게 종료
			}
		});
	}

	private void markFailedIfNotCancelled(Run run) {
		var wasCancelled = cancelledRuns.remove(run.id());
		if (!wasCancelled) {
			ExecutorSupport.updateStatus(runRepository, run, RunStatus.FAILED);
		}
	}

	private void handleTimeout(Run run, Process process) {
		process.destroyForcibly();
		var wasCancelled = cancelledRuns.remove(run.id());
		if (!wasCancelled) {
			ExecutorSupport.updateStatus(runRepository, run, RunStatus.FAILED);
			logger.warn("{} run={} timed out after {}s", engine(), run.id(), MAX_RUN_SECONDS);
		}
	}

	@Override
	public void stop(String runId) {
		cancelledRuns.add(runId);
		var process = processes.remove(runId);
		if (process != null) {
			process.destroyForcibly();
		}
		var stopped = ExecutorSupport.updateStatusToStopped(runRepository, runId);
		if (!stopped) {
			cancelledRuns.remove(runId);
		}
		logger.info("{} run={} stopped", engine(), runId);
	}
}
