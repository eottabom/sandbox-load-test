## API (Draft)

### 시나리오 관리

시나리오 목록 조회

GET /api/scenarios

```
// Response
{
    "scenarios": [
        {
            "id": "uuid",              // 시나리오 고유 식별자
            "name": "sample-test",     // 시나리오 이름
            "engine": "k6",            // 실행 엔진 (k6 | gatling)
            "description": "샘플 테스트", // 시나리오 설명
            "createdAt": "2026-02-02T12:00:00Z" // 생성 일시
        }
    ]
}
```

시나리오 상세 조회

GET /api/scenarios/{id}

```
// Response
{
    "id": "uuid",              // 시나리오 고유 식별자
    "name": "sample-test",     // 시나리오 이름
    "engine": "k6",            // 실행 엔진 (k6 | gatling)
    "content": "import http from 'k6/http';...", // 시나리오 내용
    "description": "샘플 테스트", // 시나리오 설명
    "filePath": "k6/scenarios/sample-test.js", // 저장된 파일 경로
    "createdAt": "2026-02-02T12:00:00Z" // 생성 일시
}
```

시나리오 생성

POST /api/scenarios

```
// Request
{
    "name": "sample-test",     // 시나리오 이름 (필수)
    "engine": "k6",            // 실행 엔진 (필수, k6 | gatling)
    "content": "import http from 'k6/http';...", // 시나리오 내용 (필수)
    "description": "샘플 테스트" // 시나리오 설명 (선택)
}

// Response
{
    "id": "uuid",              // 생성된 시나리오 고유 식별자
    "name": "sample-test",     // 시나리오 이름
    "engine": "k6",            // 실행 엔진
    "filePath": "k6/scenarios/sample-test.js", // 저장된 파일 경로
    "createdAt": "2026-02-02T12:00:00Z" // 생성 일시
}
```

시나리오 수정

PUT /api/scenarios/{id}

```
// Request
{
    "name": "sample-test-v2",  // 시나리오 이름 (선택)
    "content": "import http from 'k6/http';// updated...", // 시나리오 내용 (선택)
    "description": "샘플 테스트 (수정)" // 시나리오 설명 (선택)
}

// Response
{
    "id": "uuid",              // 시나리오 고유 식별자
    "name": "sample-test-v2",  // 수정된 시나리오 이름
    "engine": "k6",            // 실행 엔진 (변경 불가)
    "filePath": "k6/scenarios/sample-test-v2.js", // 저장된 파일 경로
    "updatedAt": "2026-02-02T13:00:00Z" // 수정 일시
}
```

시나리오 삭제

DELETE /api/scenarios/{id}

---

### 테스트 실행

테스트 실행

POST /api/runs

```
// Request
{
    "scenarioId": "uuid"       // 실행할 시나리오 ID (필수)
}

// Response
{
    "id": "run-uuid",          // 실행 고유 식별자
    "scenarioId": "uuid",      // 시나리오 ID
    "status": "running",       // 실행 상태 (pending | running | completed | failed | stopped)
    "startedAt": "2026-02-02T12:00:00Z" // 실행 시작 일시
}
```

실행 이력 조회

GET /api/runs

```
// Response
{
    "runs": [
        {
            "id": "run-uuid",          // 실행 고유 식별자
            "scenarioId": "uuid",      // 시나리오 ID
            "scenarioName": "sample-test", // 시나리오 이름
            "engine": "k6",            // 실행 엔진
            "status": "completed",     // 실행 상태
            "startedAt": "2026-02-02T12:00:00Z", // 실행 시작 일시
            "finishedAt": "2026-02-02T12:05:00Z" // 실행 종료 일시
        }
    ]
}
```

실행 상태 조회

GET /api/runs/{id}

```
// Response
{
    "id": "run-uuid",          // 실행 고유 식별자
    "scenarioId": "uuid",      // 시나리오 ID
    "scenarioName": "sample-test", // 시나리오 이름
    "engine": "k6",            // 실행 엔진
    "status": "completed",     // 실행 상태
    "startedAt": "2026-02-02T12:00:00Z", // 실행 시작 일시
    "finishedAt": "2026-02-02T12:05:00Z" // 실행 종료 일시
}
```

테스트 중단

POST /api/runs/{id}/stop

---

### 리포트

실행 리포트 조회

GET /api/runs/{id}/report

```
// Response
{
    "runId": "run-uuid",       // 실행 고유 식별자
    "summary": {
        "totalRequests": 15000,    // 총 요청 수
        "avgResponseTime": 123.5,  // 평균 응답 시간 (ms)
        "p95": 250.3,              // 95 백분위 응답 시간 (ms)
        "p99": 512.1,              // 99 백분위 응답 시간 (ms)
        "errorRate": 0.02          // 에러율 (0.0 ~ 1.0)
    }
}
```

AI 분석 리포트 생성

POST /api/runs/{id}/report/ai

---

### 실시간 로그

SSE(Server-Sent Events)

GET /api/runs/{id}/logs

Accept: text/event-stream