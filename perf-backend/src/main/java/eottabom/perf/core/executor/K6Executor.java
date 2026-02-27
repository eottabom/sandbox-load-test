package eottabom.perf.core.executor;

import eottabom.perf.domain.Engine;
import eottabom.perf.domain.Run;
import eottabom.perf.domain.Scenario;
import eottabom.perf.infrastructure.RunRepository;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class K6Executor extends AbstractProcessExecutor {

	public K6Executor(RunRepository runRepository) {
		super(runRepository);
	}

	@Override
	public Engine engine() {
		return Engine.K6;
	}

	@Override
	protected ProcessBuilder buildProcess(Scenario scenario, Run run, Path workDir) throws Exception {
		var script = workDir.resolve("script.js");
		Files.writeString(script, scenario.content());
		return new ProcessBuilder("k6", "run", script.toString());
	}
}
