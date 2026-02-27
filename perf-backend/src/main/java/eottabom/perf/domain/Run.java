package eottabom.perf.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("runs")
public record Run(
		@Id String id,
		String scenarioId,
		RunStatus status,
		LocalDateTime startedAt,
		LocalDateTime finishedAt
) {
}
