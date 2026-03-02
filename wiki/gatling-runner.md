어노테이션 기반으로 Gatling 부하 테스트를 실행하고, 실행 중 메트릭을 Prometheus 형식으로 노출하는 실행 JAR 모듈.

---

## 왜 만들었나

Gatling은 기본적으로 테스트가 **끝난 뒤** HTML 리포트를 생성한다.
실행 중 실시간 메트릭을 Prometheus/Grafana로 보려면 별도 설정이 필요하고, 시나리오 클래스도 Gatling의 `Simulation`을 직접 상속해야 해서 템플릿이 무거워진다.

`gatling-runner`는 두 가지 문제를 동시에 해결한다.

1. **어노테이션만 붙이면 실행** — `@LoadTest`, `@Protocol`, `@Scenario`, `@Injection` 4개 어노테이션으로 시나리오를 선언하면 `GatlingRunner`가 나머지를 처리한다. `Simulation`을 상속하거나 `setUp()`을 직접 호출할 필요가 없다.
2. **실행 중 Prometheus 메트릭 노출** — 테스트가 시작되는 순간 HTTP 엔드포인트(기본 `:9102/metrics`)가 열리고, 응답 시간·요청 수·에러 수·활성 사용자 수가 실시간으로 스크레이핑된다.

---

## 목표

- Gatling 시나리오 작성 진입 장벽 낮추기 (Simulation 상속 제거)
- 테스트 실행 중 실시간 메트릭 가시성 확보 (Prometheus + Grafana)
- `perf-backend`에서 시나리오 소스를 받아 **런타임 컴파일 → 실행**하는 파이프라인 지원
- 단일 Fat JAR로 배포 — 의존성 없이 `java -jar`로 즉시 실행 가능

---

## 전체 플로우

```
[ 시나리오 클래스 (.java) ]
         │
         │  java -jar gatling-runner.jar <ClassName | /path/to/File.java>
         ▼
[ GatlingRunner (main) ]
   ├─ .java 파일이면 RuntimeCompiler로 런타임 컴파일
   ├─ 클래스패스에 있는 클래스면 Class.forName으로 로드
   └─ DynamicPrometheusSimulation.setTargetClass(className)
         │
         │  Gatling 엔진이 DynamicPrometheusSimulation 인스턴스 생성
         ▼
[ DynamicPrometheusSimulation (생성자) ]
   ├─ SimulationAnnotationScanner → @Protocol / @Scenario / @Injection 필드 스캔
   ├─ PrometheusProtocolWrapper   → HttpProtocol에 메트릭 수집 래퍼 적용
   └─ setUp(scenario.injectOpen(injection)).protocols(wrappedProtocol)
         │
         │  테스트 실행 시작
         ▼
[ GatlingPrometheusMetrics (싱글톤) ]
   ├─ 응답마다 Histogram / Counter 기록
   └─ PrometheusServerManager → HTTP 서버 :9102 기동
         │
         ▼
[ :9102/metrics ]  ←─ Prometheus 스크레이핑
         │
         ▼
[ Grafana 대시보드 ]
```

---

## 패키지 구조

```
io.github.eottabom.gatling
├── annotation
│   ├── LoadTest      # 시나리오 클래스 마커 어노테이션
│   ├── Protocol      # HttpProtocolBuilder 필드 마커
│   ├── Scenario      # ScenarioBuilder 필드 마커
│   └── Injection     # OpenInjectionStep[] 필드 마커
├── core
│   ├── DynamicPrometheusSimulation  # Gatling이 실행하는 Simulation 구현체
│   ├── SimulationAnnotationScanner  # 어노테이션 스캔 → SimulationDescriptor 반환
│   ├── SimulationDescriptor         # 스캔 결과 값 객체 (record)
│   └── PrometheusSimulation         # 공통 Simulation 베이스 (metrics 초기화)
├── metrics
│   ├── GatlingPrometheusMetrics     # 메트릭 기록 싱글톤
│   ├── PrometheusServerManager      # HTTPServer 생명주기 관리
│   ├── PrometheusProtocolWrapper    # HttpProtocol에 메트릭 래핑
│   └── PrometheusHttpDsl            # 수동 메트릭 기록용 DSL (선택적)
├── runner
│   ├── GatlingRunner                # 진입점 main 클래스
│   └── RuntimeCompiler              # .java 파일 런타임 컴파일
└── simulations
    ├── SampleScenario               # 어노테이션 방식 예제
    └── SampleSimulation             # 기존 방식 예제 (참고용)
```

---

## 전제 조건

| 항목 | 버전 | 비고 |
|------|------|------|
| Java | 21 이상 | RuntimeCompiler는 `javac`를 PATH에서 탐색 |
| Gatling | 3.14.x | Fat JAR에 포함 (별도 설치 불필요) |
| Prometheus | 2.x | 메트릭 스크레이핑용 (선택) |
| Grafana | 10.x 이상 | 대시보드 시각화 (선택) |

> Prometheus/Grafana는 테스트 실행에 필수가 아니다. 없어도 테스트는 정상 실행된다.
> 메트릭은 `:9102/metrics`에 노출되므로 Prometheus 없이 `curl`로도 확인 가능하다.

---

## 어노테이션 기반 시나리오 작성

### 4개 어노테이션

| 어노테이션 | 대상 | 설명 |
|-----------|------|------|
| `@LoadTest` | 클래스 | 시나리오 클래스임을 선언. `name` 속성으로 시뮬레이션 이름 지정 가능 |
| `@Protocol` | 필드 (`HttpProtocolBuilder`) | 대상 URL, 헤더 등 HTTP 프로토콜 설정 |
| `@Scenario` | 필드 (`ScenarioBuilder`) | 실행할 HTTP 요청 시나리오 체인 |
| `@Injection` | 필드 (`OpenInjectionStep[]`) | 가상 사용자 주입 전략 (ramp-up, constant 등) |

### 예제

```java
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import io.github.eottabom.gatling.annotation.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

@LoadTest(name = "My Load Test")          // name 생략 시 클래스명 사용
public class MyScenario {

    @Protocol
    HttpProtocolBuilder protocol = http
        .baseUrl("https://api.example.com")
        .acceptHeader("application/json");

    @Scenario
    ScenarioBuilder scenario = scenario("Main Flow")
        .exec(http("Health Check").get("/health"))
        .pause(Duration.ofMillis(500))
        .exec(http("Get Items").get("/items"));

    @Injection
    OpenInjectionStep[] injection = {
        rampUsers(50).during(Duration.ofSeconds(30)),   // 30초 동안 50명 ramp-up
        constantUsersPerSec(10).during(Duration.ofMinutes(2)) // 이후 2분 동안 초당 10명
    };
}
```

> **규칙**
> - `@LoadTest`가 없으면 `GatlingRunner`가 실행을 거부한다.
> - `@Protocol`, `@Scenario`, `@Injection` 3개가 모두 있어야 한다.
> - 필드 접근 제어자는 무관하다 (리플렉션으로 스캔).

---

## 실행 방법

### 클래스패스에 있는 클래스 실행

```bash
# Fat JAR 빌드
./gradlew shadowJar

# 클래스명만 지정 (기본 패키지 io.github.eottabom.gatling.simulations 자동 적용)
java -jar build/libs/gatling-runner.jar SampleScenario

# 패키지 포함 전체 클래스명
java -jar build/libs/gatling-runner.jar io.github.eottabom.gatling.simulations.SampleScenario
```

### 외부 .java 파일 런타임 컴파일 후 실행

```bash
# 시나리오 파일을 작성한 뒤 경로 지정
java -jar gatling-runner.jar /tmp/MyScenario.java
```

런타임 컴파일 시 `javac`가 PATH에 있어야 하고, `gatling-runner.jar`가 컴파일 classpath에 포함된다.

### Prometheus 포트 변경

기본 포트는 `9102`. 환경변수로 변경 가능하다 (구현 확장 시 참고).

---

---

## 노출되는 Prometheus 메트릭

테스트 실행 중 `http://localhost:9102/metrics`에서 확인 가능하다.

| 메트릭 이름 | 타입 | 설명 | 라벨 |
|------------|------|------|------|
| `gatling_response_time_milliseconds` | Histogram | 응답 시간 분포 (ms) | `simulation`, `scenario`, `request`, `status` |
| `gatling_requests_total` | Counter | 총 요청 수 (성공/실패 포함) | `simulation`, `scenario`, `request`, `status` |
| `gatling_errors_total` | Counter | 총 에러 수 | `simulation`, `scenario`, `request`, `error` |
| `gatling_active_users` | Gauge | 현재 활성 사용자 수 | `simulation`, `scenario` |
| `gatling_users_started_total` | Counter | 시나리오를 시작한 총 사용자 수 | `simulation`, `scenario` |
| `gatling_users_finished_total` | Counter | 시나리오를 완료한 총 사용자 수 | `simulation`, `scenario` |

---

## 빌드 및 배포

### 로컬 빌드

```bash
cd gatling-runner
./gradlew shadowJar
# → build/libs/gatling-runner.jar
```

### GitHub Packages 배포

`main` 브랜치의 `gatling-runner/**` 경로에 변경이 푸시되면 GitHub Actions가 자동으로 빌드·배포한다.

- `version.properties`의 버전이 `X.Y.Z-SNAPSHOT`이면 패키지만 업데이트, 태그 없음
- `X.Y.Z`(SNAPSHOT 없음)이면 `gatling-runner-vX.Y.Z` 태그 생성 후 다음 SNAPSHOT으로 자동 범프

배포된 패키지는 [GitHub Packages](https://github.com/eottabom/sandbox-load-test/packages)에서 확인할 수 있다.
