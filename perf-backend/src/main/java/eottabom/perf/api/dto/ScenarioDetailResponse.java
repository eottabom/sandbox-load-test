package eottabom.perf.api.dto;

import eottabom.perf.domain.Engine;
import eottabom.perf.domain.Scenario;

import java.time.LocalDateTime;

public record ScenarioDetailResponse(
		String id,
		String name,
		Engine engine,
		String content,
		String description,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static ScenarioDetailResponse from(Scenario scenario) {
		return new ScenarioDetailResponse(
				scenario.id(),
				scenario.name(),
				scenario.engine(),
				scenario.content(),
				scenario.description(),
				scenario.createdAt(),
				scenario.updatedAt()
		);
	}
}
