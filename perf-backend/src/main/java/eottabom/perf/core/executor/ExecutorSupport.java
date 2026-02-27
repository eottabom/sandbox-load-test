package eottabom.perf.core.executor;

import eottabom.perf.domain.Run;
import eottabom.perf.domain.RunStatus;
import eottabom.perf.infrastructure.RunRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;

final class ExecutorSupport {

	private ExecutorSupport() {
	}

	static void updateStatus(RunRepository runRepository, Run run, RunStatus newStatus) {
		LocalDateTime finishedAt = newStatus.isTerminal() ? LocalDateTime.now() : run.finishedAt();
		runRepository.save(new Run(run.id(), run.scenarioId(), newStatus, run.startedAt(), finishedAt));
	}

	static void updateStatusToStopped(RunRepository runRepository, String runId) {
		runRepository.findById(runId).ifPresent(run -> updateStatus(runRepository, run, RunStatus.STOPPED));
	}

	static void deleteQuietly(Path path) {
		if (path == null) return;
		try (var paths = Files.walk(path)) {
			paths.sorted(Comparator.reverseOrder())
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
