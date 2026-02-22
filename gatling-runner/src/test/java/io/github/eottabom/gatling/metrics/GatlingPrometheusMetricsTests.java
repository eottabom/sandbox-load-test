package io.github.eottabom.gatling.metrics;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class GatlingPrometheusMetricsTests extends PrometheusContainerFixture {

	private static final GatlingPrometheusMetrics metrics = GatlingPrometheusMetrics.getInstance();

	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("scrapedMetrics")
	void metricIsScrapedByPrometheus(String metric, Runnable given) {
		// given
		given.run();
		// when: prometheus scrapes the metrics endpoint
		// then
		assertPrometheusHasMetric(metric);
	}

	static Stream<Arguments> scrapedMetrics() {
		return Stream.of(
				Arguments.of("gatling_requests_total",
						(Runnable) () -> metrics.recordRequest("SmokeSimulation", "Smoke Test", "Get Crocodiles", true, 120L)),
				Arguments.of("gatling_response_time_milliseconds_count",
						(Runnable) () -> metrics.recordRequest("SmokeSimulation", "Smoke Test", "Get Crocodiles", false, 1500L)),
				Arguments.of("gatling_errors_total",
						(Runnable) () -> metrics.recordError("SmokeSimulation", "Smoke Test", "Get Crocodile 2", "HTTP_503")),
				Arguments.of("gatling_active_users",
						(Runnable) () -> metrics.userStarted("SmokeSimulation", "Smoke Test")),
				Arguments.of("gatling_users_started_total",
						(Runnable) () -> metrics.userStarted("SmokeSimulation", "Smoke Test")),
				Arguments.of("gatling_users_finished_total",
						(Runnable) () -> metrics.userFinished("SmokeSimulation", "Smoke Test"))
		);
	}
}
