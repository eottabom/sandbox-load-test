package io.github.eottabom.gatling.core;

import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.http.HttpProtocolBuilder;

record SimulationDescriptor(
		String simulationName,
		String scenarioName,
		HttpProtocolBuilder protocol,
		ScenarioBuilder scenario,
		OpenInjectionStep[] injection
) {
	SimulationDescriptor {
		if (injection != null) {
			injection = injection.clone();
		}
	}

	@Override
	public OpenInjectionStep[] injection() {
		return injection != null ? injection.clone() : null;
	}
}
