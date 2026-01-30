package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import prometheus.PrometheusSimulation;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import static prometheus.PrometheusHttpDsl.*;

public class SmokeSimulation extends PrometheusSimulation {

    private static final String SIMULATION_NAME = "SmokeSimulation";
    private static final String SCENARIO_NAME = "Smoke Test";

    HttpProtocolBuilder httpProtocol = http
        .baseUrl("https://test-api.k6.io")
        .acceptHeader("application/json")
        .userAgentHeader("Gatling/Smoke Test");

    ScenarioBuilder scn = scenario(SCENARIO_NAME)
        .exec(trackUser(SIMULATION_NAME, SCENARIO_NAME))
        // Request 1: Get all crocodiles
        .exec(
            prometheusHttpWithCheck(
                SIMULATION_NAME,
                SCENARIO_NAME,
                http("Get Crocodiles 1").get("/public/crocodiles/"),
                "Get Crocodiles 1",
                200
            )
        )
        .pause(Duration.ofMillis(300))
        // Request 2: Get another crocodile
        .exec(
            prometheusHttpWithCheck(
                SIMULATION_NAME,
                SCENARIO_NAME,
                http("Get Crocodile 2").get("/public/crocodiles/2/"),
                "Get Crocodile 2",
                200
            )
        )
        .pause(Duration.ofMillis(200))
        // Request 3: Get crocodile 3
        .exec(
            prometheusHttpWithCheck(
                SIMULATION_NAME,
                SCENARIO_NAME,
                http("Get Crocodile 3").get("/public/crocodiles/3/"),
                "Get Crocodile 3",
                200
            )
        )
        .exec(untrackUser());

    {
        setUp(
                scn.injectOpen(
                        constantUsersPerSec(2)   // 초당 2명 유입
                                .during(Duration.ofMinutes(4))
                )
        ).protocols(httpProtocol)
         .assertions(
             global().failedRequests().count().is(0L)
         );
    }
}