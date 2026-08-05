package io.github.eottabom.gatling.core;

import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.github.eottabom.gatling.annotation.Injection;
import io.github.eottabom.gatling.annotation.LoadTest;
import io.github.eottabom.gatling.annotation.Protocol;
import io.github.eottabom.gatling.annotation.Scenario;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 픽스처 필드는 io.gatling.javaapi.http.HttpDsl({@code http(...)}) 사용을 의도적으로 피한다.
 * HttpDsl의 static 초기화 블록은 Gatling이 Simulation을 부트스트랩할 때만 채워지는 Predef 설정에
 * 접근하는데, 그 밖의 상황에서 HttpDsl을 건드리면 해당 JVM에서 이후로 계속 초기화 실패한다.
 * HttpProtocolBuilder의 public 생성자와 CoreDsl.scenario()는 이 의존성이 없어서
 * 실제 Gatling 타입을 격리된 상태에서도 스캔할 수 있다.
 */
class SimulationAnnotationScannerTests {

	private final SimulationAnnotationScanner scanner = new SimulationAnnotationScanner();

	@Test
	void scansValidClassAndUsesClassNameWhenLoadTestNameIsBlank() throws ReflectiveOperationException {
		var descriptor = scanner.scan(ValidScenario.class);

		assertThat(descriptor.simulationName()).isEqualTo("ValidScenario");
		assertThat(descriptor.scenarioName()).isEqualTo("Named Scenario");
		assertThat(descriptor.protocol()).isNotNull();
		assertThat(descriptor.scenario()).isNotNull();
		assertThat(descriptor.injection()).hasSize(1);
	}

	@Test
	void usesLoadTestNameWhenProvided() throws ReflectiveOperationException {
		var descriptor = scanner.scan(NamedScenario.class);

		assertThat(descriptor.simulationName()).isEqualTo("Custom Name");
	}

	@Test
	void wrapsSingleInjectionStepIntoArray() throws ReflectiveOperationException {
		var descriptor = scanner.scan(SingleInjectionScenario.class);

		assertThat(descriptor.injection()).hasSize(1);
	}

	@Test
	void throwsWhenProtocolFieldIsMissing() {
		assertThatThrownBy(() -> scanner.scan(MissingProtocolScenario.class))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Missing @Protocol field");
	}

	@Test
	void throwsWhenScenarioFieldIsMissing() {
		assertThatThrownBy(() -> scanner.scan(MissingScenarioScenario.class))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Missing @Scenario field");
	}

	@Test
	void throwsWhenInjectionFieldIsMissing() {
		assertThatThrownBy(() -> scanner.scan(MissingInjectionScenario.class))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Missing @Injection field");
	}

	private static HttpProtocolBuilder fakeProtocol() {
		return new HttpProtocolBuilder(null);
	}

	@LoadTest
	static class ValidScenario {
		@Protocol
		HttpProtocolBuilder protocol = fakeProtocol();

		@Scenario
		ScenarioBuilder scenario = scenario("Named Scenario");

		@Injection
		OpenInjectionStep[] injection = {constantUsersPerSec(1).during(Duration.ofSeconds(1))};
	}

	@LoadTest(name = "Custom Name")
	static class NamedScenario {
		@Protocol
		HttpProtocolBuilder protocol = fakeProtocol();

		@Scenario
		ScenarioBuilder scenario = scenario("Any");

		@Injection
		OpenInjectionStep[] injection = {constantUsersPerSec(1).during(Duration.ofSeconds(1))};
	}

	@LoadTest
	static class SingleInjectionScenario {
		@Protocol
		HttpProtocolBuilder protocol = fakeProtocol();

		@Scenario
		ScenarioBuilder scenario = scenario("Single Injection");

		@Injection
		OpenInjectionStep injection = constantUsersPerSec(1).during(Duration.ofSeconds(1));
	}

	@LoadTest
	static class MissingProtocolScenario {
		@Scenario
		ScenarioBuilder scenario = scenario("Any");

		@Injection
		OpenInjectionStep[] injection = {constantUsersPerSec(1).during(Duration.ofSeconds(1))};
	}

	@LoadTest
	static class MissingScenarioScenario {
		@Protocol
		HttpProtocolBuilder protocol = fakeProtocol();

		@Injection
		OpenInjectionStep[] injection = {constantUsersPerSec(1).during(Duration.ofSeconds(1))};
	}

	@LoadTest
	static class MissingInjectionScenario {
		@Protocol
		HttpProtocolBuilder protocol = fakeProtocol();

		@Scenario
		ScenarioBuilder scenario = scenario("Any");
	}
}
