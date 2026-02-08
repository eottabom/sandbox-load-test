package runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import prometheus.DynamicPrometheusSimulation;
import prometheus.annotation.LoadTest;
import sun.misc.Signal;

/**
 * 어노테이션 기반 Gatling 테스트 러너.
 * <p>
 * 사용법:
 * java -jar gatling-load-test.jar MyScenario
 * java -jar gatling-load-test.jar scenarios.MyScenario
 */
public class GatlingRunner {

	private static final Logger log = LoggerFactory.getLogger(GatlingRunner.class);

	private static final String DEFAULT_PACKAGE = "simulations";

	public static void main(String[] args) {
		Signal.handle(new Signal("TSTP"), _ -> {
			log.info("\nCtrl+Z detected - shutting down (port cleanup)...");
			System.exit(0);
		});

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
		// 패키지 없으면 기본 패키지 추가
		String className = classNameInput.contains(".")
				? classNameInput
				: DEFAULT_PACKAGE + "." + classNameInput;

		log.info("Starting gatling {}", className);

		Class<?> clazz = Class.forName(className);

		if (!clazz.isAnnotationPresent(LoadTest.class)) {
			throw new IllegalArgumentException(
					"Class " + className + " must have @LoadTest annotation"
			);
		}

		// 클래스명만 저장 (인스턴스화는 Gatling 런타임 내부에서)
		DynamicPrometheusSimulation.setTargetClass(className);

		log.info("Starting gatling {}", className);

		// Gatling 실행 → Gatling이 DynamicPrometheusSimulation 로드
		io.gatling.app.Gatling.main(new String[]{
				"--simulation", DynamicPrometheusSimulation.class.getName(),
				"--results-folder", "results"
		});
	}

	private static void printUsage() {
		log.info("Usage: java -jar gatling-load-test.jar <scenario-class>");
		log.info("");
		log.info("Examples:");
		log.info("  java -jar gatling-load-test.jar MyScenario");
		log.info("  java -jar gatling-load-test.jar scenarios.MyScenario");
		log.info("");
		log.info("Scenario class must:");
		log.info("  - Have @LoadTest annotation");
		log.info("  - Have @Protocol field (HttpProtocolBuilder)");
		log.info("  - Have @Scenario field (ScenarioBuilder)");
		log.info("  - Have @Injection field (OpenInjectionStep[])");
	}
}