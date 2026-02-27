package eottabom.perf.api.dto;

import eottabom.perf.domain.Engine;
import eottabom.perf.domain.Scenario;

import java.time.LocalDateTime;

public record ScenarioResponse(
		String id,
		String name,
		Engine engine,
		String description,
		LocalDateTime createdAt
) {
	public static ScenarioResponse from(Scenario scenario) {
		return new ScenarioResponse(
				scenario.id(),
				scenario.name(),
				scenario.engine(),
				scenario.description(),
				scenario.createdAt()
		);
	}
}
