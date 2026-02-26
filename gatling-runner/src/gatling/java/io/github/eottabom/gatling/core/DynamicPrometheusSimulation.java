package io.github.eottabom.gatling.core;

import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.github.eottabom.gatling.metrics.PrometheusProtocolWrapper;

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
			Class<?> clazz = loadClass();
			SimulationDescriptor descriptor = new SimulationAnnotationScanner().scan(clazz);

			HttpProtocolBuilder wrappedProtocol = PrometheusProtocolWrapper.wrap(
					descriptor.protocol(), metrics, descriptor.simulationName(), descriptor.scenarioName()
			);

			setUp(
					descriptor.scenario().injectOpen(descriptor.injection())
			).protocols(wrappedProtocol);

		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to load scenario class: " + targetClassName, e);
		}
	}

	private Class<?> loadClass() throws ClassNotFoundException {
		return targetClassLoader != null
				? Class.forName(targetClassName, true, targetClassLoader)
				: Class.forName(targetClassName);
	}
}
