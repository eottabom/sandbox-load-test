package io.github.eottabom.gatling.metrics;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GatlingPrometheusMetrics {

	private static final Logger log = LoggerFactory.getLogger(GatlingPrometheusMetrics.class);
	private static final int DEFAULT_PORT = 9102;
	private static final String UNKNOWN = "unknown";
	private static volatile GatlingPrometheusMetrics instance;
	private static final Object lock = new Object();
	private static volatile boolean metricsRegistered = false;

	private final PrometheusRegistry registry;
	private final PrometheusServerManager serverManager;

	private Histogram responseTimeHistogram;
	private Counter requestCounter;
	private Counter errorCounter;
	private Gauge activeUsersGauge;
	private Counter usersStartedCounter;
	private Counter usersFinishedCounter;
	private final ConcurrentHashMap<ActiveKey, AtomicInteger> activeUsersByKey = new ConcurrentHashMap<>();

	private GatlingPrometheusMetrics() {
		this.registry = PrometheusRegistry.defaultRegistry;
		this.serverManager = new PrometheusServerManager(registry);
		registerMetrics();
		serverManager.registerShutdownHook();
		autoStartServer();
	}

	private void autoStartServer() {
		try {
			serverManager.startServer(DEFAULT_PORT);
		} catch (IOException e) {
			log.error("Could not start Prometheus server: {}", e.getMessage());
			throw new IllegalStateException("Failed to start Prometheus metrics server on port "
					+ DEFAULT_PORT + ". Check port availability.", e);
		}
	}

	private void registerMetrics() {
		if (metricsRegistered) {
			log.debug("Prometheus metrics already registered, skipping...");
			return;
		}

		try {
			// JVM metrics
			JvmMetrics.builder().register(registry);
		} catch (Exception e) {
			// JVM metrics already registered, ignore
		}

		try {
			// Response time histogram with buckets (matching Gatling's default ranges)
			this.responseTimeHistogram = Histogram.builder()
					.name("gatling_response_time_milliseconds")
					.help("Response time in milliseconds")
					.labelNames("simulation", "scenario", "request", "status")
					.classicUpperBounds(100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100, 1200, 1500, 2000, 3000, 5000, 10000)
					.register(registry);

			// Request counter
			this.requestCounter = Counter.builder()
					.name("gatling_requests_total")
					.help("Total number of requests")
					.labelNames("simulation", "scenario", "request", "status")
					.register(registry);

			// Error counter
			this.errorCounter = Counter.builder()
					.name("gatling_errors_total")
					.help("Total number of errors")
					.labelNames("simulation", "scenario", "request", "error")
					.register(registry);

			// Active users gauge
			this.activeUsersGauge = Gauge.builder()
					.name("gatling_active_users")
					.help("Number of active users")
					.labelNames("simulation", "scenario")
					.register(registry);

			// Users started counter
			this.usersStartedCounter = Counter.builder()
					.name("gatling_users_started_total")
					.help("Total number of users started")
					.labelNames("simulation", "scenario")
					.register(registry);

			// Users finished counter
			this.usersFinishedCounter = Counter.builder()
					.name("gatling_users_finished_total")
					.help("Total number of users finished")
					.labelNames("simulation", "scenario")
					.register(registry);

			metricsRegistered = true;
			log.info("Prometheus metrics registered successfully");
		} catch (Exception e) {
			log.error("Error registering metrics: {}", e.getMessage());
			throw new RuntimeException("Failed to register Prometheus metrics", e);
		}
	}

	public static GatlingPrometheusMetrics getInstance() {
		if (instance == null) {
			synchronized (lock) {
				if (instance == null) {
					instance = new GatlingPrometheusMetrics();
				}
			}
		}
		return instance;
	}

	public void recordRequest(String simulation, String scenario, String request,
							  boolean success, long responseTimeMs) {
		String status = success ? "ok" : "ko";
		String safeSimulation = normalizeLabel(simulation);
		String safeScenario = normalizeLabel(scenario);
		String safeRequest = normalizeLabel(request);

		responseTimeHistogram
				.labelValues(safeSimulation, safeScenario, safeRequest, status)
				.observe(responseTimeMs);

		requestCounter
				.labelValues(safeSimulation, safeScenario, safeRequest, status)
				.inc();
	}

	public void recordError(String simulation, String scenario, String request, String errorMessage) {
		String safeSimulation = normalizeLabel(simulation);
		String safeScenario = normalizeLabel(scenario);
		String safeRequest = normalizeLabel(request);
		String safeError = normalizeLabel(errorMessage);
		errorCounter
				.labelValues(safeSimulation, safeScenario, safeRequest, safeError)
				.inc();
	}

	public void userStarted(String simulation, String scenario) {
		String safeSimulation = normalizeLabel(simulation);
		String safeScenario = normalizeLabel(scenario);
		ActiveKey key = new ActiveKey(safeSimulation, safeScenario);
		AtomicInteger count = activeUsersByKey.computeIfAbsent(key, k -> new AtomicInteger(0));
		count.incrementAndGet();
		activeUsersGauge
				.labelValues(safeSimulation, safeScenario)
				.inc();
		usersStartedCounter
				.labelValues(safeSimulation, safeScenario)
				.inc();
	}

	public void userFinished(String simulation, String scenario) {
		String safeSimulation = normalizeLabel(simulation);
		String safeScenario = normalizeLabel(scenario);
		ActiveKey key = new ActiveKey(safeSimulation, safeScenario);
		AtomicInteger count = activeUsersByKey.computeIfAbsent(key, k -> new AtomicInteger(0));
		int prev = count.getAndUpdate(current -> current > 0 ? current - 1 : 0);
		if (prev > 0) {
			activeUsersGauge
					.labelValues(safeSimulation, safeScenario)
					.dec();
		} else {
			log.debug("Active users already at 0 for {} / {}, skipping decrement",
					safeSimulation, safeScenario);
		}
		usersFinishedCounter
				.labelValues(safeSimulation, safeScenario)
				.inc();
	}

	private String normalizeLabel(String value) {
		if (value == null) {
			return UNKNOWN;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? UNKNOWN : trimmed;
	}

	private record ActiveKey(String simulation, String scenario) {
	}
}
