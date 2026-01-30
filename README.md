> ⚠️ **Work in Progress (WIP)**  
> 이 프로젝트는 현재 **개발 및 아키텍처 실험 단계**에 있으며,  
> 구조 검증과 아이디어 구현을 목적으로 지속적으로 변경되고 있습니다.  
> 프로덕션 사용을 전제로 하지 않습니다.

# Performace Testing & AI Analysis Platform

이 저장소는 부하 테스트 실행, 실시간 성능 메트릭 수집, 그리고 AI 기반 성능 분석 리포트 생성을 하나의 흐름으로 통합한 성능 테스트 플랫폼 PoC 입니다. 

## 목적
* 다양한 부하 테스트를 일관된 방식으로 실행
* 테스트 중 발생하는 메트릭을 실시간으로 시각화
* 수집된 데이터 기반으로 AI 성능 분석 리포트 생성
* 테스트 도구에 종속되지 않는 확장 가능한 구조


## 전체 아키텍처 개요

<img width="1040" height="1063" alt="image" src="https://github.com/user-attachments/assets/2d73b85c-f1c2-4901-9307-178cb1468829" />

## TODO

```
perf-aigent-platform/
├── docker-compose/             # 인프라 설정 (기존 k6-stack 기반)
│   ├── docker-compose.yml      # InfluxDB, Grafana, Prometheus 등
│   └── grafana/                # 그라파나 프로비저닝 설정 (Dashboards, Datasources)
│       └── provisioning/
├── perf-frontend/              # Next.js 프로젝트
│   ├── src/
│   │   ├── components/         # Monaco Editor, Grafana Iframe 등
│   │   └── hooks/              # WebSocket 연동 (테스트 실시간 로그)
│   └── public/
├── perf-backend/               # Spring Boot (Java 17/21 + Spring AI)
│   ├── src/main/java/com/perf/
│   │   ├── api/                # 컨트롤러 (테스트 실행/중단, 리포트 조회)
│   │   ├── core/               # 핵심 비즈니스 로직
│   │   │   ├── executor/       # [핵심] k6, Gatling 실행 전략 (Strategy Pattern)
│   │   │   ├── validator/      # 스크립트 검증 로직
│   │   │   └── reporter/       # Spring AI 기반 리포트 생성기
│   │   ├── infrastructure/     # InfluxDB Client, Docker API 연동
│   │   └── model/              # TestJob, Metrics, Report 엔티티
│   └── src/main/resources/
│       └── application.yml
├── perf-executor/              # 테스트 실행 스크립트 저장소 및 런타임 환경
│   ├── k6/
│   │   ├── scripts/            # 유저가 업로드/작성한 k6 스크립트
│   │   └── lib/                # 공통 JS 모듈
│   └── gatling/
│       ├── simulations/        # 게틀링 Scala/Java 시뮬레이션 파일
│       └── conf/               # gatling.conf, logback.xml
└── README.md
```
