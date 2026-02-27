package eottabom.perf.core.executor;

import eottabom.perf.domain.Engine;
import eottabom.perf.domain.Run;
import eottabom.perf.domain.Scenario;

public interface TestExecutor {
	Engine engine();

	void execute(Scenario scenario, Run run);

	void stop(String runId);
}
