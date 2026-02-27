package eottabom.perf.api.dto;

import eottabom.perf.domain.Engine;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateScenarioRequest(
		@NotBlank String name,
		@NotNull Engine engine,
		@NotBlank String content,
		String description
) {
}
