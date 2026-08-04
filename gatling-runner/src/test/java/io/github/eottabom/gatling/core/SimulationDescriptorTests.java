package io.github.eottabom.gatling.core;

import io.gatling.javaapi.core.OpenInjectionStep;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static org.assertj.core.api.Assertions.assertThat;

class SimulationDescriptorTests {

	@Test
	void defensivelyCopiesInjectionArrayOnConstructionAndAccess() {
		var original = new OpenInjectionStep[]{constantUsersPerSec(1).during(Duration.ofSeconds(1))};

		var descriptor = new SimulationDescriptor("sim", "scenario", null, null, original);
		original[0] = null;

		assertThat(descriptor.injection()[0]).isNotNull();

		var firstRead = descriptor.injection();
		firstRead[0] = null;

		assertThat(descriptor.injection()[0]).isNotNull();
	}

	@Test
	void allowsNullInjection() {
		var descriptor = new SimulationDescriptor("sim", "scenario", null, null, null);

		assertThat(descriptor.injection()).isNull();
	}
}
