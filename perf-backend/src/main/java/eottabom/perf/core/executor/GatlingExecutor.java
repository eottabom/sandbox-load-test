package eottabom.perf.core.executor;

import eottabom.perf.domain.Engine;
import eottabom.perf.domain.Run;
import eottabom.perf.domain.RunStatus;
import eottabom.perf.domain.Scenario;
import eottabom.perf.infrastructure.RunRepository;
import io.github.eottabom.gatling.runner.GatlingRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class GatlingExecutor implements TestExecutor {

	private static final Logger logger = LoggerFactory.getLogger(GatlingExecutor.class);
	private static final Pattern CLASS_NAME_PATTERN =
			Pattern.compile("public\\s+(?:final\\s+|abstract\\s+)?class\\s+(\\w+)");

	private final RunRepository runRepository;
	private final ConcurrentHashMap<String, Thread> threads = new ConcurrentHashMap<>();
	private final Set<String> cancelledRuns = ConcurrentHashMap.newKeySet();

	public GatlingExecutor(RunRepository runRepository) {
		this.runRepository = runRepository;
	}

	@Override
	public Engine engine() {
		return Engine.GATLING;
	}

	@Override
	public void execute(Scenario scenario, Run run) {
		var thread = new Thread(() -> runInThread(scenario, run));
		thread.setDaemon(true);
		threads.put(run.id(), thread);
		thread.start();
	}

	private void runInThread(Scenario scenario, Run run) {
		Path workDir = null;
		try {
			workDir = Files.createTempDirectory("perf-" + run.id() + "-");

			// stop()이 먼저 호출된 경우 중단
			if (cancelledRuns.remove(run.id())) {
				return;
			}

			var className = extractClassName(scenario.content());
			var sourceFile = workDir.resolve(className + ".java");
			Files.writeString(sourceFile, scenario.content());

			ExecutorSupport.updateStatus(runRepository, run, RunStatus.RUNNING);

			var exitCode = runGatling(sourceFile.toString());

			if (removeCurrentThread(run.id())) {
				var cancelledAtFinish = cancelledRuns.remove(run.id());
				if (!cancelledAtFinish) {
					ExecutorSupport.updateStatus(runRepository, run, ExecutorSupport.toRunStatus(exitCode));
					logger.info("{} run={} finished exitCode={}", engine().name(), run.id(), exitCode);
				}
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			cancelledRuns.remove(run.id());
			ExecutorSupport.updateStatus(runRepository, run, RunStatus.STOPPED);
		} catch (Exception ex) {
			logger.error("{} run={} failed: {}", engine().name(), run.id(), ex.getMessage(), ex);
			var wasCancelled = cancelledRuns.remove(run.id());
			if (wasCancelled) {
				ExecutorSupport.updateStatus(runRepository, run, RunStatus.STOPPED);
			} else {
				ExecutorSupport.updateStatus(runRepository, run, RunStatus.FAILED);
			}
		} finally {
			removeCurrentThread(run.id());
			cancelledRuns.remove(run.id());
			ExecutorSupport.deleteQuietly(workDir);
		}
	}

	@Override
	public void stop(String runId) {
		cancelledRuns.add(runId);
		var thread = threads.get(runId);
		if (thread != null && threads.remove(runId, thread)) {
			thread.interrupt();
		}
		var stopped = ExecutorSupport.updateStatusToStopped(runRepository, runId);
		if (!stopped) {
			cancelledRuns.remove(runId);
		}
		logger.info("{} run={} stop requested", engine().name(), runId);
	}

	int runGatling(String sourceFile) throws Exception {
		return GatlingRunner.runWithoutExit(sourceFile);
	}

	private boolean removeCurrentThread(String runId) {
		return threads.remove(runId, Thread.currentThread());
	}

	private String extractClassName(String source) {
		var matcher = CLASS_NAME_PATTERN.matcher(source);
		if (matcher.find()) {
			return matcher.group(1);
		}
		throw new IllegalArgumentException(
				"Cannot extract class name from scenario source. Content must contain 'public class ClassName' declaration.");
	}
}