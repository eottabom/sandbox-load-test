package io.github.eottabom.gatling.metrics;

import io.gatling.javaapi.http.HttpProtocolBuilder;

public class PrometheusProtocolWrapper {

	private PrometheusProtocolWrapper() {
	}

	public static HttpProtocolBuilder wrap(HttpProtocolBuilder protocol,
										   GatlingPrometheusMetrics metrics,
										   String simulationName,
										   String scenarioName) {
		return protocol.transformResponse((response, _) -> {
			String requestName = response.request().getName();
			if (requestName == null || requestName.isEmpty()) {
				requestName = "unknown";
			}

			int statusCode = response.status().code();
			long responseTime = Math.max(0, response.endTimestamp() - response.startTimestamp());
			boolean success = statusCode >= 200 && statusCode < 400;

			metrics.recordRequest(simulationName, scenarioName, requestName, success, responseTime);

			if (!success) {
				metrics.recordError(simulationName, scenarioName, requestName, "HTTP_" + statusCode);
			}

			return response;
		});
	}
}
