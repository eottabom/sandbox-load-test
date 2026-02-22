package io.github.eottabom.gatling.runner;

import io.gatling.app.Gatling;
import io.github.eottabom.gatling.annotation.LoadTest;
import io.github.eottabom.gatling.core.DynamicPrometheusSimulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 어노테이션 기반 Gatling 테스트 러너.
 * <p>
 * 사용법:
 + java -jar gatling-runner-1.0.0.jar MyScenario
 + java -jar gatling-runner-1.0.0.jar scenarios.MyScenario
 + java -jar gatling-runner-1.0.0.jar /tmp/SampleScenario.java
 */
public class GatlingRunner {

	private static final Logger log = LoggerFactory.getLogger(GatlingRunner.class);

	private static final String DEFAULT_PACKAGE = "io.github.eottabom.gatling.simulations";

	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread(() ->
				log.info("Shutdown hook triggered - cleaning up...")
		));

		if (args.length < 1) {
			printUsage();
			System.exit(1);
		}

		try {
			run(args[0]);
		} catch (Exception e) {
			log.error("Error: {}, {}", e.getMessage(), e.getStackTrace());
			System.exit(1);
		}
	}

	public static void run(String classNameInput) throws Exception {
		Class<?> clazz;
		ClassLoader customClassLoader = null;

		if (classNameInput.endsWith(".java")) {
			// 외부 .java 파일 런타임 컴파일
			Path sourceFile = Path.of(classNameInput);
			if (!Files.exists(sourceFile)) {
				throw new IllegalArgumentException("Source file not found: " + classNameInput);
			}
			clazz = RuntimeCompiler.compile(sourceFile);
			customClassLoader = clazz.getClassLoader();
		} else {
			// 기존 로직: 클래스패스에서 로드
			String className = classNameInput.contains(".")
					? classNameInput
					: DEFAULT_PACKAGE + "." + classNameInput;
			clazz = Class.forName(className);
		}

		String className = clazz.getName();
		log.info("Starting gatling {}", className);

		if (!clazz.isAnnotationPresent(LoadTest.class)) {
			throw new IllegalArgumentException(
					"Class " + className + " must have @LoadTest annotation"
			);
		}

		// 클래스명 및 ClassLoader 저장 (인스턴스화는 Gatling 런타임 내부에서)
		if (customClassLoader != null) {
			DynamicPrometheusSimulation.setTargetClass(className, customClassLoader);
		} else {
			DynamicPrometheusSimulation.setTargetClass(className);
		}

		// Gatling 실행 → Gatling이 DynamicPrometheusSimulation 로드
		Gatling.main(new String[]{
				"--simulation", DynamicPrometheusSimulation.class.getName(),
				"--results-folder", "results"
		});
	}

	private static void printUsage() {
		log.info("Usage: java -jar gatling-load-test.jar <scenario-class|source-file>");
		log.info("");
		log.info("Examples:");
		log.info("  java -jar gatling-load-test.jar MyScenario");
		log.info("  java -jar gatling-load-test.jar scenarios.MyScenario");
		log.info("  java -jar gatling-load-test.jar /tmp/SampleScenario.java");
		log.info("");
		log.info("Scenario class must:");
		log.info("  - Have @LoadTest annotation");
		log.info("  - Have @Protocol field (HttpProtocolBuilder)");
		log.info("  - Have @Scenario field (ScenarioBuilder)");
		log.info("  - Have @Injection field (OpenInjectionStep[])");
	}
}
