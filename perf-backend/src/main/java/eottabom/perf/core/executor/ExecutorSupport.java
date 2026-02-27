package eottabom.perf.core.executor;

import eottabom.perf.domain.Run;
import eottabom.perf.domain.RunStatus;
import eottabom.perf.infrastructure.RunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;

final class ExecutorSupport {

	private static final Logger logger = LoggerFactory.getLogger(ExecutorSupport.class);

	private ExecutorSupport() {
	}

	static void updateStatus(RunRepository runRepository, Run run, RunStatus newStatus) {
		LocalDateTime finishedAt = newStatus.isTerminal() ? LocalDateTime.now() : run.finishedAt();
		runRepository.save(new Run(run.id(), run.scenarioId(), newStatus, run.startedAt(), finishedAt));
	}

	static boolean updateStatusToStopped(RunRepository runRepository, String runId) {
		var target = runRepository.findById(runId)
				.filter(run -> !run.status().isTerminal())
				.orElse(null);
		if (target == null) {
			return false;
		}
		updateStatus(runRepository, target, RunStatus.STOPPED);
		return true;
	}

	static void deleteQuietly(Path path) {
		if (path == null) return;
		try (var paths = Files.walk(path)) {
			paths.sorted(Comparator.reverseOrder())
					.forEach(p -> {
						try {
							Files.delete(p);
						} catch (IOException ex) {
							logger.debug("Failed to delete {}: {}", p, ex.getMessage());
						}
					});
		} catch (IOException ex) {
			logger.debug("Failed to walk {}: {}", path, ex.getMessage());
		}
	}
}
