package prometheus;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Session;

import java.util.function.Function;

import static io.gatling.javaapi.core.CoreDsl.*;

public class PrometheusActionBuilder {

    private static final GatlingPrometheusMetrics metrics = GatlingPrometheusMetrics.getInstance();

    public static ChainBuilder recordMetrics(String simulation, String scenario, String requestName) {
        return exec(session -> {
            long startTime = session.getLong("prometheus_start_time");
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;

            boolean success = session.getBoolean("prometheus_request_success");

            metrics.recordRequest(simulation, scenario, requestName, success, responseTime);

            if (!success) {
                String errorMsg = session.getString("prometheus_error_message");
                if (errorMsg == null) {
                    errorMsg = "request_failed";
                }
                metrics.recordError(simulation, scenario, requestName, errorMsg);
            }

            return session;
        });
    }

    public static ChainBuilder startTimer() {
        return exec(session -> session.set("prometheus_start_time", System.currentTimeMillis()));
    }

    public static ChainBuilder markSuccess() {
        return exec(session -> session.set("prometheus_request_success", true));
    }

    public static ChainBuilder markFailure(String errorMessage) {
        return exec(session -> session
                .set("prometheus_request_success", false)
                .set("prometheus_error_message", errorMessage));
    }

    public static Function<Session, Session> userStarted(String simulation, String scenario) {
        return session -> {
            metrics.userStarted(simulation, scenario);
            return session;
        };
    }

    public static Function<Session, Session> userFinished(String simulation, String scenario) {
        return session -> {
            metrics.userFinished(simulation, scenario);
            return session;
        };
    }
}