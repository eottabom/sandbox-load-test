package prometheus;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

public class GatlingPrometheusMetrics {

    private static final Logger log = LoggerFactory.getLogger(GatlingPrometheusMetrics.class);
    private static final int DEFAULT_PORT = 9102;
    private static volatile GatlingPrometheusMetrics instance;
    private static final Object lock = new Object();
    private static volatile boolean shutdownHookRegistered = false;
    private static volatile boolean metricsRegistered = false;

    private final PrometheusRegistry registry;
    private volatile HTTPServer server;
    private volatile int currentPort = -1;

    private Histogram responseTimeHistogram;
    private Counter requestCounter;
    private Counter errorCounter;
    private Gauge activeUsersGauge;
    private Counter usersStartedCounter;
    private Counter usersFinishedCounter;

    private GatlingPrometheusMetrics() {
        this.registry = PrometheusRegistry.defaultRegistry;
        registerMetrics();
        registerShutdownHook();
        autoStartServer();
    }

    private void autoStartServer() {
        try {
            startServer(DEFAULT_PORT);
        } catch (IOException e) {
            log.warn("Could not start Prometheus server: {}", e.getMessage());
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

    private void registerShutdownHook() {
        if (!shutdownHookRegistered) {
            shutdownHookRegistered = true;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutdown hook triggered - stopping Prometheus server...");
                stopServerInternal();
            }, "prometheus-shutdown-hook"));
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

    public void startServer(int port) throws IOException {
        synchronized (lock) {
            if (server != null) {
                log.debug("Prometheus metrics server already running on port {}", currentPort);
                return;
            }

            int targetPort = port;
            try {
                server = HTTPServer.builder()
                        .port(targetPort)
                        .registry(registry)
                        .buildAndStart();
            } catch (IOException e) {
                // 기본 포트 실패시 랜덤 포트 사용
                targetPort = getRandomAvailablePort();
                log.info("Port {} unavailable, using random port {}", port, targetPort);
                server = HTTPServer.builder()
                        .port(targetPort)
                        .registry(registry)
                        .buildAndStart();
            }
            currentPort = targetPort;
            log.info("Prometheus metrics server started - http://localhost:{}/metrics", currentPort);
        }
    }

    private int getRandomAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private void stopServerInternal() {
        if (server != null) {
            try {
                server.close();
                log.info("Prometheus metrics server stopped");
            } catch (Exception e) {
                log.error("Error stopping Prometheus server: {}", e.getMessage());
            } finally {
                server = null;
                currentPort = -1;
            }
        }
    }

    public void recordRequest(String simulation, String scenario, String request,
                              boolean success, long responseTimeMs) {
        String status = success ? "ok" : "ko";

        responseTimeHistogram
                .labelValues(simulation, scenario, request, status)
                .observe(responseTimeMs);

        requestCounter
                .labelValues(simulation, scenario, request, status)
                .inc();
    }

    public void recordError(String simulation, String scenario, String request, String errorMessage) {
        String safeError = errorMessage != null ?
                errorMessage.substring(0, Math.min(100, errorMessage.length())) : "unknown";
        errorCounter
                .labelValues(simulation, scenario, request, safeError)
                .inc();
    }

    public void userStarted(String simulation, String scenario) {
        activeUsersGauge
                .labelValues(simulation, scenario)
                .inc();
        usersStartedCounter
                .labelValues(simulation, scenario)
                .inc();
    }

    public void userFinished(String simulation, String scenario) {
        activeUsersGauge
                .labelValues(simulation, scenario)
                .dec();
        usersFinishedCounter
                .labelValues(simulation, scenario)
                .inc();
    }
}