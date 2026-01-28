# Gatling + InfluxDB Integration

## Overview
Gatling 테스트 결과를 InfluxDB로 전송하고 Grafana 대시보드에서 시각화합니다.

## Prerequisites
- Docker Compose로 인프라 실행 중 (`docker compose up -d`)
- Gatling 3.x 프로젝트 (sbt 또는 Maven)

## Setup

### 1. Dependency 추가

**sbt** (`build.sbt`):
```scala
libraryDependencies += "io.perfana" % "gatling-to-influxdb" % "1.0.0" % "test"
```

**Maven** (`pom.xml`):
```xml
<dependency>
    <groupId>io.perfana</groupId>
    <artifactId>gatling-to-influxdb</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

### 2. gatling.conf 설정

`src/test/resources/gatling.conf`:
```hocon
gatling {
  data {
    writers = [console, file, influxdb]
  }
}

influxdb {
  url = "http://localhost:8086"
  database = "gatling"
}
```

### 3. 테스트 실행

```bash
# sbt
sbt "Gatling/testOnly simulations.YourSimulation"

# Maven
mvn gatling:test -Dgatling.simulationClass=simulations.YourSimulation
```

## Grafana Dashboard

- URL: http://localhost:3000
- Dashboard: "Load Testing" > "Gatling"
- Datasource: InfluxDB-Gatling

## Alternative: x2i tool

Perfana의 [x2i](https://github.com/perfana/x2i) 도구를 사용해 기존 Gatling 로그를 InfluxDB로 가져올 수도 있습니다:

```bash
# simulation.log 파일을 InfluxDB로 import
x2i gatling -i simulation.log -o http://localhost:8086 -d gatling
```

## References
- [gatling-to-influxdb](https://github.com/perfana/gatling-to-influxdb)
- [x2i](https://github.com/perfana/x2i)
- [Grafana Dashboard 21776](https://grafana.com/grafana/dashboards/21776-gatling/)