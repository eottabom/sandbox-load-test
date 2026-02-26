package io.github.eottabom.gatling.core;

import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.github.eottabom.gatling.annotation.Injection;
import io.github.eottabom.gatling.annotation.LoadTest;
import io.github.eottabom.gatling.annotation.Protocol;
import io.github.eottabom.gatling.annotation.Scenario;

import java.lang.reflect.Field;

class SimulationAnnotationScanner {

	/**
	 * Scans the given simulation class for {@code @Protocol}, {@code @Scenario}, and {@code @Injection} fields.
	 * <p>Note: instantiates the target class via its no-arg constructor to read field values.
	 * If that constructor has side effects (e.g. network calls, file I/O), callers must be aware.
	 */
	SimulationDescriptor scan(Class<?> clazz) throws ReflectiveOperationException {
		Object instance = clazz.getDeclaredConstructor().newInstance();

		LoadTest loadTestAnnotation = clazz.getAnnotation(LoadTest.class);
		String simName = (loadTestAnnotation != null && !loadTestAnnotation.name().isEmpty())
				? loadTestAnnotation.name()
				: clazz.getSimpleName();

		HttpProtocolBuilder protocol = null;
		ScenarioBuilder scenario = null;
		OpenInjectionStep[] injection = null;
		String scenarioName = null;

		for (Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);

			if (field.isAnnotationPresent(Protocol.class)) {
				Object value = field.get(instance);
				if (value instanceof HttpProtocolBuilder pb) {
					protocol = pb;
				}
			}

			if (field.isAnnotationPresent(Scenario.class)) {
				Object value = field.get(instance);
				if (value instanceof ScenarioBuilder sb) {
					scenario = sb;
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

		validate(clazz.getName(), protocol, scenario, injection);

		if (scenarioName == null || scenarioName.isEmpty()) {
			scenarioName = simName + " Scenario";
		}

		return new SimulationDescriptor(simName, scenarioName, protocol, scenario, injection);
	}

	private String extractScenarioName(ScenarioBuilder scenario) {
		try {
			Field nameField = scenario.wrapped.getClass().getDeclaredField("name");
			nameField.setAccessible(true);
			Object value = nameField.get(scenario.wrapped);
			return value instanceof String s ? s : null;
		} catch (ReflectiveOperationException e) {
			return null;
		}
	}

	private void validate(String className, HttpProtocolBuilder protocol,
						  ScenarioBuilder scenario, OpenInjectionStep[] injection) {
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
					"Invalid configuration in " + className + ":\n" + errors
			);
		}
	}
}
