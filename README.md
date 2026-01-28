<img width="1040" height="1063" alt="image" src="https://github.com/user-attachments/assets/2d73b85c-f1c2-4901-9307-178cb1468829" />


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
