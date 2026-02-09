package prometheus;

import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import prometheus.annotation.Injection;
import prometheus.annotation.LoadTest;
import prometheus.annotation.Protocol;
import prometheus.annotation.Scenario;

import java.lang.reflect.Field;

public class DynamicPrometheusSimulation extends PrometheusSimulation {

	private static volatile String targetClassName;
	private static volatile ClassLoader targetClassLoader;

	public static void setTargetClass(String className) {
		targetClassName = className;
		targetClassLoader = null;
	}

	public static void setTargetClass(String className, ClassLoader classLoader) {
		targetClassName = className;
		targetClassLoader = classLoader;
	}

	public DynamicPrometheusSimulation() {
		if (targetClassName == null || targetClassName.isEmpty()) {
			throw new IllegalStateException(
					"Target class not set. Use GatlingRunner to run tests."
			);
		}

		try {
			// Gatling 런타임 내부에서 리플렉션 수행
			Class<?> clazz = targetClassLoader != null
					? Class.forName(targetClassName, true, targetClassLoader)
					: Class.forName(targetClassName);
			Object instance = clazz.getDeclaredConstructor().newInstance();

			// 시뮬레이션 이름
			LoadTest loadTestAnnotation = clazz.getAnnotation(LoadTest.class);
			String simName = (loadTestAnnotation != null && !loadTestAnnotation.name().isEmpty())
					? loadTestAnnotation.name()
					: clazz.getSimpleName();

			// 필드 스캔
			HttpProtocolBuilder protocol = null;
			ScenarioBuilder scenario = null;
			OpenInjectionStep[] injection = null;
			String scenarioName = null;

			for (Field field : clazz.getDeclaredFields()) {
				field.setAccessible(true);

				if (field.isAnnotationPresent(Protocol.class)) {
					protocol = (HttpProtocolBuilder) field.get(instance);
				}

				if (field.isAnnotationPresent(Scenario.class)) {
					Object value = field.get(instance);
					if (value instanceof ScenarioBuilder sb) {
						scenario = sb;
						// 시나리오 이름 추출 시도
						scenarioName = extractScenarioName(sb);
					}
				}

				if (field.isAnnotationPresent(Injection.class)) {
					Object value = field.get(instance);
					if (value instanceof OpenInjectionStep[] steps) {
						injection = steps;
					} else if (value instanceof OpenInjectionStep step) {
						injection = new OpenInjectionStep[]{step};
					}
				}
			}

			// 검증
			validateConfiguration(protocol, scenario, injection);

			if (scenarioName == null || scenarioName.isEmpty()) {
				scenarioName = simName + " Scenario";
			}

			// Protocol 래핑 - 자동 메트릭 수집
			final String finalSimName = simName;
			final String finalScenarioName = scenarioName;
			final GatlingPrometheusMetrics metrics = this.metrics;

			HttpProtocolBuilder wrappedProtocol = protocol
					.transformResponse((response, session) -> {
						String requestName = response.request().getName();
						if (requestName == null || requestName.isEmpty()) {
							requestName = "unknown";
						}

						int statusCode = response.status().code();
						long responseTime = response.endTimestamp() - response.startTimestamp();
						boolean success = statusCode >= 200 && statusCode < 400;

						metrics.recordRequest(finalSimName, finalScenarioName, requestName, success, responseTime);

						if (!success) {
							metrics.recordError(finalSimName, finalScenarioName, requestName, "HTTP_" + statusCode);
						}

						return response;
					});

			// 시뮬레이션 설정
			setUp(
					scenario.injectOpen(injection)
			).protocols(wrappedProtocol);

		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to load scenario class: " + targetClassName, e);
		}
	}

	private String extractScenarioName(ScenarioBuilder scenario) {
		try {
			Field nameField = scenario.getClass().getDeclaredField("name");
			nameField.setAccessible(true);
			return (String) nameField.get(scenario);
		} catch (Exception e) {
			return null;
		}
	}

	private void validateConfiguration(HttpProtocolBuilder protocol,
									   ScenarioBuilder scenario,
									   OpenInjectionStep[] injection) {
		StringBuilder errors = new StringBuilder();

		if (protocol == null) {
			errors.append("  - Missing @Protocol field (HttpProtocolBuilder)\n");
		}
		if (scenario == null) {
			errors.append("  - Missing @Scenario field (ScenarioBuilder)\n");
		}
		if (injection == null) {
			errors.append("  - Missing @Injection field (OpenInjectionStep[] or OpenInjectionStep)\n");
		}

		if (!errors.isEmpty()) {
			throw new IllegalStateException(
					"Invalid configuration in " + targetClassName + ":\n" + errors
			);
		}
	}
}
