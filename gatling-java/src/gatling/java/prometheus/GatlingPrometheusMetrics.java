package prometheus;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

import java.io.IOException;
import java.net.ServerSocket;

public class GatlingPrometheusMetrics {

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
    }

    private void registerMetrics() {
        if (metricsRegistered) {
            System.out.println("Prometheus metrics already registered, skipping...");
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
            System.out.println("Prometheus metrics registered successfully");
        } catch (Exception e) {
            System.err.println("Error registering metrics: " + e.getMessage());
            throw new RuntimeException("Failed to register Prometheus metrics", e);
        }
    }

    private void registerShutdownHook() {
        if (!shutdownHookRegistered) {
            shutdownHookRegistered = true;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutdown hook triggered - stopping Prometheus server...");
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

    /**
     * 인스턴스 리셋 - 테스트 재시작 시 호출
     */
    public static void reset() {
        synchronized (lock) {
            if (instance != null) {
                instance.stopServerInternal();
            }
            // 인스턴스는 유지하고 서버만 재시작 가능하도록
        }
    }

    public void startServer(int port) throws IOException {
        synchronized (lock) {
            // 이미 같은 포트에서 실행 중
            if (server != null && currentPort == port) {
                System.out.println("Prometheus metrics server already running on port " + port);
                return;
            }

            // 다른 포트에서 실행 중이면 종료
            if (server != null) {
                stopServerInternal();
            }

            // 포트 사용 가능 여부 확인 및 대기
            int maxRetries = 5;
            int retryCount = 0;

            while (!isPortAvailable(port) && retryCount < maxRetries) {
                System.out.println("Port " + port + " is in use. Waiting... (" + (retryCount + 1) + "/" + maxRetries + ")");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for port " + port);
                }
                retryCount++;
            }

            if (!isPortAvailable(port)) {
                // 포트가 여전히 사용 중이면 다른 포트 시도
                int alternativePort = findAvailablePort(port + 1, port + 100);
                if (alternativePort > 0) {
                    System.out.println("Port " + port + " unavailable. Using alternative port " + alternativePort);
                    port = alternativePort;
                } else {
                    throw new IOException("Port " + port + " is still in use and no alternative port available.");
                }
            }

            try {
                server = HTTPServer.builder()
                        .port(port)
                        .registry(registry)
                        .buildAndStart();
                currentPort = port;
                System.out.println("✓ Prometheus metrics server started on port " + port);
                System.out.println("  Metrics available at: http://localhost:" + port + "/metrics");
            } catch (IOException e) {
                System.err.println("Failed to start Prometheus server on port " + port + ": " + e.getMessage());
                throw e;
            }
        }
    }

    private int findAvailablePort(int startPort, int endPort) {
        for (int port = startPort; port <= endPort; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        return -1;
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void stopServer() {
        synchronized (lock) {
            stopServerInternal();
        }
    }

    private void stopServerInternal() {
        if (server != null) {
            try {
                server.close();
                System.out.println("Prometheus metrics server stopped");
            } catch (Exception e) {
                System.err.println("Error stopping Prometheus server: " + e.getMessage());
            } finally {
                server = null;
                currentPort = -1;
            }
        }
    }

    public boolean isServerRunning() {
        return server != null;
    }

    public int getCurrentPort() {
        return currentPort;
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

    public Histogram getResponseTimeHistogram() {
        return responseTimeHistogram;
    }

    public Counter getRequestCounter() {
        return requestCounter;
    }

    public Gauge getActiveUsersGauge() {
        return activeUsersGauge;
    }
}