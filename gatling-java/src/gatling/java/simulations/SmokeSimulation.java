package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class SmokeSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
        .baseUrl("https://test-api.k6.io")
        .acceptHeader("application/json")
        .userAgentHeader("Gatling/Smoke Test");

    ScenarioBuilder scn = scenario("Smoke Test")
        .exec(
            http("Test")
                .get("/public/crocodiles/")
                .check(status().is(200))
        );

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