package eottabom.perf.api.dto;

import jakarta.validation.constraints.NotBlank;

public record StartRunRequest(
		@NotBlank String scenarioId
) {
}
