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

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
abstract class PrometheusContainerFixture {

	private static final HttpClient httpClient = HttpClient.newHttpClient();

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
		for (int i = 0; i < 15; i++) {
			var body = fetchBody(url, metric);
			if (!body.contains("\"result\":[]")) {
				return body;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new AssertionError("Interrupted while polling metric: " + metric, ex);
			}
		}
		return fetchBody(url, metric);
	}

	private String fetchBody(String url, String metric) {
		try {
			return httpClient.send(
					HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
					HttpResponse.BodyHandlers.ofString()
			).body();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Interrupted while querying metric: " + metric, ex);
		} catch (IOException ex) {
			throw new AssertionError("Failed to query Prometheus for metric: " + metric, ex);
		}
	}
}
