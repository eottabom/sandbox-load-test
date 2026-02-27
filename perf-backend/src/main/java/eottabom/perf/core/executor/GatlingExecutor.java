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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GatlingExecutor implements TestExecutor {

	private static final Logger logger = LoggerFactory.getLogger(GatlingExecutor.class);
	private static final Pattern CLASS_NAME_PATTERN =
			Pattern.compile("public\\s+(?:final\\s+|abstract\\s+)?class\\s+(\\w+)");

	private final RunRepository runRepository;
	private final ConcurrentHashMap<String, Thread> threads = new ConcurrentHashMap<>();

	public GatlingExecutor(RunRepository runRepository) {
		this.runRepository = runRepository;
	}

	@Override
	public Engine engine() {
		return Engine.GATLING;
	}

	@Override
	public void execute(Scenario scenario, Run run) {
		var thread = new Thread(() -> {
			var workDir = (Path) null;
			try {
				workDir = Files.createTempDirectory("perf-" + run.id() + "-");
				var className = extractClassName(scenario.content());
				var sourceFile = workDir.resolve(className + ".java");
				Files.writeString(sourceFile, scenario.content());

				ExecutorSupport.updateStatus(runRepository, run, RunStatus.RUNNING);

				var exitCode = GatlingRunner.runWithoutExit(sourceFile.toString());

				if (threads.remove(run.id(), Thread.currentThread())) {
					var result = exitCode == 0 ? RunStatus.COMPLETED : RunStatus.FAILED;
					ExecutorSupport.updateStatus(runRepository, run, result);
					logger.info("GATLING run={} finished exitCode={}", run.id(), exitCode);
				}
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				// stop()이 이미 map에서 제거하고 STOPPED 업데이트를 처리했으므로 여기서는 skip
				if (threads.remove(run.id(), Thread.currentThread())) {
					ExecutorSupport.updateStatus(runRepository, run, RunStatus.STOPPED);
				}
			} catch (Exception ex) {
				logger.error("GATLING run={} failed: {}", run.id(), ex.getMessage(), ex);
				if (threads.remove(run.id(), Thread.currentThread())) {
					ExecutorSupport.updateStatus(runRepository, run, RunStatus.FAILED);
				}
			} finally {
				threads.remove(run.id(), Thread.currentThread());
				ExecutorSupport.deleteQuietly(workDir);
			}
		});
		thread.setDaemon(true);
		threads.put(run.id(), thread);
		thread.start();
	}

	@Override
	public void stop(String runId) {
		var thread = threads.get(runId);
		var isRemoved = threads.remove(runId, thread);

		if (thread != null && isRemoved) {
			thread.interrupt();
			ExecutorSupport.updateStatusToStopped(runRepository, runId);
			logger.info("GATLING run={} stop requested", runId);
		}
	}

	private String extractClassName(String source) {
		Matcher matcher = CLASS_NAME_PATTERN.matcher(source);
		if (matcher.find()) {
			return matcher.group(1);
		}
		throw new IllegalArgumentException(
				"Cannot extract class name from scenario source. Content must contain 'public class ClassName' declaration.");
	}
}
