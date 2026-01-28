# Gatling Java + InfluxDB

Gradle 기반 Gatling Java 프로젝트입니다.

## Requirements

- Java 17+
- Gradle 8.x (wrapper 포함)

## Project Structure

```
gatling-java/
├── build.gradle
├── settings.gradle
└── src/gatling/
    ├── java/simulations/
    │   ├── SampleSimulation.java
    │   └── SmokeSimulation.java
    └── resources/
        ├── gatling.conf
        └── logback.xml
```

## Usage

### Run Simulation

```bash
# 기본 시뮬레이션 실행
./gradlew gatlingRun

# 특정 시뮬레이션 실행
./gradlew gatlingRun-simulations.SampleSimulation
./gradlew gatlingRun-simulations.SmokeSimulation
```

### InfluxDB 연동

k6-stack의 InfluxDB로 메트릭을 전송하려면:

1. k6-stack에서 Graphite input 활성화 필요 (InfluxDB 설정)
2. 또는 [perfana/x2i](https://github.com/perfana/x2i) 도구로 결과 파일 import

```bash
# x2i로 simulation.log를 InfluxDB로 import
x2i gatling -i build/reports/gatling/*/simulation.log \
    -o http://localhost:8086 -d gatling
```

## Reports

테스트 완료 후 HTML 리포트 위치:
```
build/reports/gatling/<simulation-name>-<timestamp>/index.html
```