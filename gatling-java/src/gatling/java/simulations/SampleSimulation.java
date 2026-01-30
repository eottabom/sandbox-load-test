package simulations;//package simulations;
//
//import io.gatling.javaapi.core.*;
//import io.gatling.javaapi.http.*;
//
//import static io.gatling.javaapi.core.CoreDsl.*;
//import static io.gatling.javaapi.http.HttpDsl.*;
//
//public class SampleSimulation extends Simulation {
//
//    HttpProtocolBuilder httpProtocol = http
//        .baseUrl("https://test-api.k6.io")
//        .acceptHeader("application/json")
//        .userAgentHeader("Gatling/Performance Test");
//
//    ScenarioBuilder scn = scenario("Sample Scenario")
//        .exec(
//            http("Get Public Crocodiles")
//                .get("/public/crocodiles/")
//                .check(status().is(200))
//        )
//        .exec(
//            http("Get Crocodile by ID")
//                .get("/public/crocodiles/1/")
//                .check(status().is(200))
//                .check(jsonPath("$.name").exists())
//        );
//
//    {
//        setUp(
//            scn.injectOpen(atOnceUsers(3))
//        ).protocols(httpProtocol)
//         .assertions(
//             global().responseTime().max().lt(2000),
//             global().successfulRequests().percent().gt(95.0)
//         );
//    }
//}