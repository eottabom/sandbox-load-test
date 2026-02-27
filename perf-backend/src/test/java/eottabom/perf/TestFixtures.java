package eottabom.perf;

import eottabom.perf.domain.Engine;
import eottabom.perf.domain.Run;
import eottabom.perf.domain.RunStatus;
import eottabom.perf.domain.Scenario;

import java.time.LocalDateTime;

public class TestFixtures {

	public static final LocalDateTime FIXED_TIME = LocalDateTime.of(2024, 1, 1, 0, 0);

	public static Scenario k6Scenario() {
		return new Scenario("s-1", "Load Test", Engine.K6, "script", "desc", FIXED_TIME, null);
	}

	public static Scenario gatlingScenario() {
		return new Scenario("s-1", "Load Test", Engine.GATLING, "content", "desc", FIXED_TIME, null);
	}

	public static Scenario scenario(String id, String name, Engine engine) {
		return new Scenario(id, name, engine, "script", "desc", FIXED_TIME, null);
	}

	public static Scenario scenario(String id, String name, Engine engine, String content, String description) {
		return new Scenario(id, name, engine, content, description, FIXED_TIME, null);
	}

	public static Run pendingRun() {
		return new Run("r-1", "s-1", RunStatus.PENDING, FIXED_TIME, null);
	}

	public static Run runningRun() {
		return new Run("r-1", "s-1", RunStatus.RUNNING, FIXED_TIME, null);
	}

	public static Run completedRun() {
		return new Run("r-1", "s-1", RunStatus.COMPLETED, FIXED_TIME, FIXED_TIME.plusHours(1));
	}

	public static Run failedRun() {
		return new Run("r-1", "s-1", RunStatus.FAILED, FIXED_TIME, FIXED_TIME.plusMinutes(1));
	}
}
