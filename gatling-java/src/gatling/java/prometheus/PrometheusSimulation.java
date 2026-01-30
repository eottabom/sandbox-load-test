package prometheus;

import io.gatling.javaapi.core.Simulation;

import java.io.IOException;

public abstract class PrometheusSimulation extends Simulation {

    protected static final int PROMETHEUS_PORT = 9102;

    protected final String simulationName;
    protected final GatlingPrometheusMetrics metrics;

    public PrometheusSimulation() {
        this.simulationName = this.getClass().getSimpleName();
        this.metrics = GatlingPrometheusMetrics.getInstance();
    }

    @Override
    public void before() {
        System.out.println("========================================");
        System.out.println("Starting simulation: " + simulationName);
        System.out.println("========================================");

        try {
            metrics.startServer(PROMETHEUS_PORT);
        } catch (IOException e) {
            System.err.println("Warning: Could not start Prometheus server on port " + PROMETHEUS_PORT);
            System.err.println("Reason: " + e.getMessage());
            System.err.println("Metrics collection will continue, but may use a different port.");

            // 대체 포트로 시도
            try {
                int altPort = findAvailablePort();
                if (altPort > 0) {
                    metrics.startServer(altPort);
                }
            } catch (IOException e2) {
                System.err.println("Failed to start on alternative port. Continuing without Prometheus HTTP server.");
            }
        }
    }

    @Override
    public void after() {
        System.out.println("========================================");
        System.out.println("Simulation completed: " + simulationName);
        System.out.println("========================================");

        // 마지막 스크래핑을 위해 잠시 대기
        int waitSeconds = 10;
        System.out.println("Waiting " + waitSeconds + " seconds for final Prometheus scrape...");

        try {
            for (int i = waitSeconds; i > 0; i--) {
                System.out.print("\rShutting down in " + i + " seconds... ");
                Thread.sleep(1000);
            }
            System.out.println();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("\nInterrupted during shutdown wait.");
        }

        // 서버 종료
        if (metrics.isServerRunning()) {
            System.out.println("Stopping Prometheus metrics server...");
            metrics.stopServer();
        }

        System.out.println("Cleanup complete.");
    }

    private int findAvailablePort() {
        for (int port = PROMETHEUS_PORT + 1; port < PROMETHEUS_PORT + 100; port++) {
            try (java.net.ServerSocket socket = new java.net.ServerSocket(port)) {
                socket.setReuseAddress(true);
                return port;
            } catch (IOException e) {
                // continue
            }
        }
        return -1;
    }

    protected GatlingPrometheusMetrics getMetrics() {
        return metrics;
    }

    protected String getSimulationName() {
        return simulationName;
    }
}