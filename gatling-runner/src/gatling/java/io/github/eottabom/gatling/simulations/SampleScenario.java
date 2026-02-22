package io.github.eottabom.gatling.simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import io.github.eottabom.gatling.annotation.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * 샘플 부하 테스트 시나리오.
 *
 * 실행: java -jar gatling-runner-1.0.0.jar SampleScenario
 * (java --add-opens java.base/java.lang=ALL-UNNAMED --enable-native-access=ALL-UNNAMED -jar gatling-runner-1.0.0.jar io.github.eottabom.gatling.simulations.SampleScenario)
 */
@LoadTest
public class SampleScenario {

    @Protocol
    HttpProtocolBuilder protocol = http
        .baseUrl("https://test-api.k6.io")
        .acceptHeader("application/json")
        .userAgentHeader("Gatling/Sample Test");

    @Scenario
    ScenarioBuilder scenario = scenario("Sample Test")
        .exec(http("Get Crocodiles").get("/public/crocodiles/"))
        .pause(Duration.ofMillis(300))
        .exec(http("Get Crocodile 2").get("/public/crocodiles/2/"))
        .pause(Duration.ofMillis(200))
        .exec(http("Get Crocodile 3").get("/public/crocodiles/3/"));

    @Injection
    OpenInjectionStep[] injection = {
        constantUsersPerSec(2).during(Duration.ofMinutes(1))
    };
}