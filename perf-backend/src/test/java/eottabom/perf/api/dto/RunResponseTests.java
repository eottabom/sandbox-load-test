package eottabom.perf.api.dto;

import eottabom.perf.TestFixtures;
import eottabom.perf.domain.Engine;
import eottabom.perf.domain.RunStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RunResponseTests {

	@Test
	void fromMapsAllFields() {
		// given
		var now = LocalDateTime.of(2024, 1, 1, 0, 0);

		// when
		var result = RunResponse.from(TestFixtures.runningRun(), TestFixtures.gatlingScenario());

		// then
		assertThat(result.id()).isEqualTo("r-1");
		assertThat(result.scenarioId()).isEqualTo("s-1");
		assertThat(result.scenarioName()).isEqualTo("Load Test");
		assertThat(result.engine()).isEqualTo(Engine.GATLING);
		assertThat(result.status()).isEqualTo(RunStatus.RUNNING);
		assertThat(result.startedAt()).isEqualTo(now);
		assertThat(result.finishedAt()).isNull();
	}

	@Test
	void fromIncludesFinishedAtWhenCompleted() {
		// given
		var startedAt = LocalDateTime.of(2024, 1, 1, 0, 0);
		var finishedAt = startedAt.plusHours(1);

		// when
		var result = RunResponse.from(TestFixtures.completedRun(), TestFixtures.k6Scenario());

		// then
		assertThat(result.status()).isEqualTo(RunStatus.COMPLETED);
		assertThat(result.finishedAt()).isEqualTo(finishedAt);
	}
}
