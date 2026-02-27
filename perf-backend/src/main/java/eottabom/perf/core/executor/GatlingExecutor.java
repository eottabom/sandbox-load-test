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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GatlingExecutor implements TestExecutor {

	private static final Logger logger = LoggerFactory.getLogger(GatlingExecutor.class);

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

				updateStatus(run, RunStatus.RUNNING);

				var exitCode = GatlingRunner.runWithoutExit(sourceFile.toString());

				if (threads.remove(run.id(), Thread.currentThread())) {
					var result = exitCode == 0 ? RunStatus.COMPLETED : RunStatus.FAILED;
					updateStatus(run, result);
					logger.info("GATLING run={} finished exitCode={}", run.id(), exitCode);
				}
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				updateStatus(run, RunStatus.STOPPED);
			} catch (Exception ex) {
				logger.error("GATLING run={} failed: {}", run.id(), ex.getMessage(), ex);
				updateStatus(run, RunStatus.FAILED);
			} finally {
				deleteQuietly(workDir);
			}
		});
		thread.setDaemon(true);
		threads.put(run.id(), thread);
		thread.start();
	}

	@Override
	public void stop(String runId) {
		var thread = threads.get(runId);
		if (thread != null && threads.remove(runId, thread)) {
			thread.interrupt();
			logger.info("GATLING run={} stop requested", runId);
		}
	}

	private String extractClassName(String source) {
		Matcher matcher = Pattern.compile("public\\s+class\\s+(\\w+)").matcher(source);
		if (matcher.find()) {
			return matcher.group(1);
		}
		throw new IllegalArgumentException("Cannot extract class name from scenario source");
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
