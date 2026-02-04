package prometheus;

import io.gatling.javaapi.core.Simulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PrometheusSimulation extends Simulation {

    private static final Logger log = LoggerFactory.getLogger(PrometheusSimulation.class);

    protected final String simulationName;
    protected final GatlingPrometheusMetrics metrics;

    public PrometheusSimulation() {
        this.simulationName = this.getClass().getSimpleName();
        this.metrics = GatlingPrometheusMetrics.getInstance();
    }

    @Override
    public void before() {
        log.info("Starting simulation: {}", simulationName);
    }

    @Override
    public void after() {
        log.info("Simulation completed: {}", simulationName);
    }
}