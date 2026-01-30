package prometheus;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class PrometheusHttpDsl {

    private static final GatlingPrometheusMetrics metrics = GatlingPrometheusMetrics.getInstance();

    public static ChainBuilder prometheusHttp(String simulation, String scenario,
                                               HttpRequestActionBuilder request, String requestName) {
        return exec(session -> session.set("prometheus_req_start", System.currentTimeMillis()))
                .exec(request
                        .check(status().saveAs("prometheus_status"))
                        .check(responseTimeInMillis().saveAs("prometheus_response_time")))
                .exec(session -> {
                    int status = session.getInt("prometheus_status");
                    long responseTime = session.getLong("prometheus_response_time");
                    boolean success = status >= 200 && status < 400;

                    metrics.recordRequest(simulation, scenario, requestName, success, responseTime);

                    if (!success) {
                        metrics.recordError(simulation, scenario, requestName, "HTTP_" + status);
                    }

                    return session;
                });
    }

    public static ChainBuilder prometheusHttpWithCheck(String simulation, String scenario,
                                                        HttpRequestActionBuilder request,
                                                        String requestName,
                                                        int expectedStatus) {
        return exec(session -> session.set("prometheus_req_start", System.currentTimeMillis()))
                .exec(request
                        .check(status().is(expectedStatus).saveAs("prometheus_status"))
                        .check(responseTimeInMillis().saveAs("prometheus_response_time")))
                .exec(session -> {
                    int status = session.getInt("prometheus_status");
                    long responseTime = session.getLong("prometheus_response_time");
                    boolean success = status == expectedStatus;

                    metrics.recordRequest(simulation, scenario, requestName, success, responseTime);

                    if (!success) {
                        metrics.recordError(simulation, scenario, requestName, "HTTP_" + status);
                    }

                    return session;
                });
    }

    public static ChainBuilder trackUser(String simulation, String scenario) {
        return exec(session -> {
            metrics.userStarted(simulation, scenario);
            return session.set("prometheus_user_tracked", true)
                    .set("prometheus_simulation", simulation)
                    .set("prometheus_scenario", scenario);
        });
    }

    public static ChainBuilder untrackUser() {
        return exec(session -> {
            String simulation = session.getString("prometheus_simulation");
            String scenario = session.getString("prometheus_scenario");
            if (simulation != null && scenario != null) {
                metrics.userFinished(simulation, scenario);
            }
            return session;
        });
    }
}