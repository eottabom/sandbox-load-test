package io.github.eottabom.gatling.metrics;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
abstract class PrometheusContainerFixture {

	static {
		org.testcontainers.Testcontainers.exposeHostPorts(9102);
	}

	private static final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

	@Container
	static GenericContainer<?> prometheus = new GenericContainer<>(DockerImageName.parse("prom/prometheus:v3.2.1"))
			.withCopyFileToContainer(
					MountableFile.forClasspathResource("prometheus-test.yml"),
					"/etc/prometheus/prometheus.yml"
			)
			.withExposedPorts(9090)
			.waitingFor(Wait.forHttp("/-/healthy").forPort(9090).withStartupTimeout(Duration.ofSeconds(60)));

	protected void assertPrometheusHasMetric(String metric) {
		String result = waitForMetric(metric);
		// then
		assertThat(result)
				.contains("\"status\":\"success\"")
				.doesNotContain("\"result\":[]");
	}

	private String waitForMetric(String metric) {
		var url = "http://%s:%d/api/v1/query?query=%s".formatted(
				prometheus.getHost(),
				prometheus.getMappedPort(9090),
				metric
		);
		return await()
				.atMost(15, TimeUnit.SECONDS)
				.pollInterval(1, TimeUnit.SECONDS)
				.until(() -> fetchBody(url, metric), body -> body.contains("\"status\":\"success\"") && !body.contains("\"result\":[]"));
	}

	private String fetchBody(String url, String metric) {
		try {
			return httpClient.send(
					HttpRequest.newBuilder()
							.uri(URI.create(url))
							.timeout(Duration.ofSeconds(5))
							.GET()
							.build(),
					HttpResponse.BodyHandlers.ofString()
			).body();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupted while querying metric: " + metric, ex);
		} catch (IOException ex) {
			return "";
		}
	}
}
