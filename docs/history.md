# History

---

## Gatling 3.12 이후 메트릭 수집 방식의 변화

[Gatling 3.12](https://docs.gatling.io/release-notes/gatling/whats-new/3.12/) 버전부터 내부 엔진에서 **Akka 의존성이 제거**되었다.

이 때, Akka 기반으로 구현되어 있던 **Graphite DataWriter 또한 함께 제거**되어,

Graphite를 통해 실시간 또는 준실시간 메트릭을 수집하던 방식은 더 이상 사용할 수 없게 되었다.

이로 인해 Gatling OSS 환경에서는 테스트 실행 중 메트릭을 외부 시계열 저장소로 스트리밍하는

**공식적인 방법이 사라졌고**, 대체 수단을 별도로 설계해야 한다.

(Gatling 에서도 Enterprise 버전으로 실시간 스트리밍을 제안한다.)

## x2i 를 활용한 대안과 한계

대안으로 [x2i](https://github.com/perfana/x2i)를 활용하면  

Gatling, JMeter, k6 등의 실행 결과 로그를 파싱하여  

InfluxDB와 같은 시계열 데이터베이스에 적재할 수 있다.

x2i 는 여러 테스트 도구를 공통 포맷으로 처리 가능하고 설정이 비교적 단순하다는 장점이 있다.

하지만, 로그 파일 기반 파싱, 내부 버퍼링 후 배치 단위 데이터 전송, 테스트 종료 시점 또는 일정 주기마다 메트릭 적재라는

**실시간에 가깝기보다는 히스토리성 또는 준실시간 수준** 으로 파악되어, 개인적인 요구사항에 한계가 존재했다.

---

## TestContainers + Docker Desktop 호환성 이슈 (2026-02)

### 배경

`gatling-runner` 모듈에 TestContainers 기반 테스트를 추가하면서,
Prometheus 컨테이너를 띄워 Gatling 메트릭 적재를 검증하는 과정에서 두 가지 호환성 문제가 발생했다.

---

### 문제 1: Docker 소켓 심볼릭 링크로 인한 전략 감지 실패

**증상**

```
java.lang.IllegalStateException: Could not find a valid Docker environment.
    at org.testcontainers.dockerclient.DockerClientProviderStrategy.lambda$getFirstValidStrategy$7
```

**원인**

macOS Docker Desktop은 컨텍스트 방식으로 소켓을 관리한다.

```
/var/run/docker.sock  →  (symlink)  →  ~/.docker/run/docker.sock
```

TestContainers의 `UnixSocketClientProviderStrategy`는 소켓 파일의 모드를 직접 검사(`srwxr-xr-x` 기대)하는데,
`/var/run/docker.sock`은 심볼릭 링크(`lrwxr-xr-x`)이므로 검사를 통과하지 못하고 전략이 실패한다.

**해결**

`src/test/resources/testcontainers.properties`에서 `DockerDesktopClientProviderStrategy`를 명시적으로 지정한다.
이 전략은 `user.home + /.docker/run/docker.sock` 경로를 직접 사용하므로 심볼릭 링크 문제를 우회한다.

```properties
docker.client.strategy=org.testcontainers.dockerclient.DockerDesktopClientProviderStrategy
```

---

### 문제 2: shaded docker-java API 버전 불일치

**증상**

```
com.github.dockerjava.api.exception.BadRequestException:
  Status 400: {"message":"client version 1.32 is too old.
  Minimum supported API version is 1.44, please upgrade your client"}
```

**원인**

TestContainers는 내부적으로 docker-java를 shaded(내장) 버전으로 번들링한다.
TestContainers 1.21.3에 번들된 shaded docker-java는 Docker API 기본 버전으로 `1.32`를 사용하는데,
Docker Desktop 4.56+ (Docker Engine 29.0+) 부터 최소 지원 API 버전이 `1.44`로 상향되어
모든 `1.44` 미만 요청을 400으로 거부한다.

- 관련 PR: [testcontainers-java #11216 - Set default docker API version to 1.44](https://github.com/testcontainers/testcontainers-java/pull/11216)
- 관련 포럼: [TestContainer stopped working after updating Docker Desktop to v4.56.0](https://forums.docker.com/t/testcontainer-stopped-working-after-updating-docker-desktop-to-v4-56-0/150823/2)

**해결**

`build.gradle`의 `test` 태스크에 JVM 시스템 프로퍼티로 API 버전을 명시한다.
shaded docker-java는 `api.version` 시스템 프로퍼티를 읽어 Docker API 버전을 재정의한다.

```gradle
test {
    useJUnitPlatform()
    jvmArgs '-Dapi.version=1.44'
    environment "TESTCONTAINERS_RYUK_DISABLED", "true"
}
```

> `TESTCONTAINERS_RYUK_DISABLED=true`는 Ryuk 정리 컨테이너가 동일한 API 버전 문제로
> JVM 종료 시 `Thread-6` 오류 출력하는 부작용을 억제하기 위해 함께 설정한다.