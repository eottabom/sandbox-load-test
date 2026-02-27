package eottabom.perf.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("scenarios")
public record Scenario(
		@Id String id,
		String name,
		Engine engine,
		String content,
		String description,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
