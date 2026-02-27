package eottabom.perf.core.executor;

import eottabom.perf.domain.Run;
import eottabom.perf.domain.RunStatus;
import eottabom.perf.domain.Scenario;
import eottabom.perf.infrastructure.RunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

abstract class AbstractProcessExecutor implements TestExecutor {

	private static final Logger logger = LoggerFactory.getLogger(AbstractProcessExecutor.class);

	private static final ExecutorService IO_EXECUTOR =
			Executors.newCachedThreadPool(r -> {
				var t = new Thread(r);
				t.setDaemon(true);
				return t;
			});

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
		IO_EXECUTOR.submit(() -> {
			var workDir = (Path) null;
			try {
				workDir = Files.createTempDirectory("perf-" + run.id() + "-");

				// PENDING 상태에서 stop()이 먼저 호출된 경우 — stop()이 이미 STOPPED 업데이트
				if (cancelledRuns.remove(run.id())) {
					return;
				}

				ExecutorSupport.updateStatus(runRepository, run, RunStatus.RUNNING);

				var process = buildProcess(scenario, run, workDir)
						.redirectErrorStream(true)
						.start();
				processes.put(run.id(), process);

				// put 직후 재확인: stop()이 processes를 찾지 못하고 지나쳤을 경우
				if (cancelledRuns.remove(run.id())) {
					if (processes.remove(run.id(), process)) {
						process.destroyForcibly();
					}
					ExecutorSupport.updateStatus(runRepository, run, RunStatus.STOPPED);
					return;
				}

				// TODO: LogStore에 연결해 SSE로 실시간 스트리밍
				process.getInputStream().transferTo(OutputStream.nullOutputStream());

				var exitCode = process.waitFor();

				// stop()이 먼저 제거한 경우 상태 업데이트 중복 방지
				if (processes.remove(run.id(), process)) {
					var result = exitCode == 0 ? RunStatus.COMPLETED : RunStatus.FAILED;
					ExecutorSupport.updateStatus(runRepository, run, result);
					logger.info("{} run={} finished exitCode={}", engine(), run.id(), exitCode);
				}
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				ExecutorSupport.updateStatus(runRepository, run, RunStatus.FAILED);
			} catch (Exception ex) {
				logger.error("{} run={} failed: {}", engine(), run.id(), ex.getMessage(), ex);
				ExecutorSupport.updateStatus(runRepository, run, RunStatus.FAILED);
			} finally {
				ExecutorSupport.deleteQuietly(workDir);
			}
		});
	}

	@Override
	public void stop(String runId) {
		cancelledRuns.add(runId);
		var process = processes.remove(runId);
		if (process != null) {
			process.destroyForcibly();
		}
		ExecutorSupport.updateStatusToStopped(runRepository, runId);
		logger.info("{} run={} stopped", engine(), runId);
	}
}
