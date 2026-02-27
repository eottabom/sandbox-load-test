package eottabom.perf.api.dto;

public record UpdateScenarioRequest(
		String name,
		String content,
		String description
) {
}
