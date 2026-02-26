package io.github.eottabom.gatling.metrics;

import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.BindException;
import java.util.concurrent.atomic.AtomicBoolean;

class PrometheusServerManager {

	private static final Logger log = LoggerFactory.getLogger(PrometheusServerManager.class);
	private static final AtomicBoolean shutdownHookRegistered = new AtomicBoolean(false);

	private final Object lock = new Object();
	private final PrometheusRegistry registry;
	private volatile HTTPServer server;
	private volatile int currentPort = -1;

	PrometheusServerManager(PrometheusRegistry registry) {
		this.registry = registry;
	}

	void startServer(int port) throws IOException {
		synchronized (lock) {
			if (server != null) {
				log.debug("Prometheus metrics server already running on port {}", currentPort);
				return;
			}

			try {
				server = HTTPServer.builder()
						.port(port)
						.registry(registry)
						.buildAndStart();
			} catch (BindException e) {
				throw new IOException(
						"Port " + port + " is already in use; cannot start Prometheus HTTP server.", e);
			}
			currentPort = port;
			log.info("Prometheus metrics server started - http://localhost:{}/metrics", currentPort);
		}
	}

	void stopServer() {
		synchronized (lock) {
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
	}

	void registerShutdownHook() {
		if (shutdownHookRegistered.compareAndSet(false, true)) {
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				log.info("Shutdown hook triggered - stopping Prometheus server...");
				stopServer();
			}, "prometheus-shutdown-hook"));
		}
	}
}
