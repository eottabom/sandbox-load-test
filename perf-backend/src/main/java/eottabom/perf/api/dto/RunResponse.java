package eottabom.perf.api.dto;

import eottabom.perf.domain.Engine;
import eottabom.perf.domain.Run;
import eottabom.perf.domain.RunStatus;
import eottabom.perf.domain.Scenario;

import java.time.LocalDateTime;

public record RunResponse(
		String id,
		String scenarioId,
		String scenarioName,
		Engine engine,
		RunStatus status,
		LocalDateTime startedAt,
		LocalDateTime finishedAt
) {
	public static RunResponse from(Run run, Scenario scenario) {
		return new RunResponse(
				run.id(),
				run.scenarioId(),
				scenario.name(),
				scenario.engine(),
				run.status(),
				run.startedAt(),
				run.finishedAt()
		);
	}
}
