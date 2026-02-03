
### FLOW

Simulation → 
PrometheusHttpDsl → 
GatlingPrometheusMetrics → 
Prometheus Registry → 
HTTP Server (9102) → 
Grafana/Prometheus

### 노출되는 주묘 메트릭

|메트릭 이름|타입|설명|라벨|
|--|--|--|--|
| `gatling_response_time_milliseconds` | Histogram | 응답 시간 분포 (ms) | `simulation`, `scenario`, `request`, `status` |
| `gatling_requests_total` | Counter | 총 요청 수 (성공/실패 포함) | `simulation`, `scenario`, `request`, `status` |
│ `gatling_errors_total` | Counter | 총 에러 수 | `simulation`, `scenario`, `request`, `error` |
│ `gatling_active_users` | Gauge | 현재 활성 사용자 수 | `simulation`, `scenario` |
│ `gatling_users_started_total` | Counter | 시나리오를 시작한 총 사용자 수 | `simulation`, `scenario` |
│ `gatling_users_finished_total`| Counter | 시나리오를 완료한 총 사용자 수 | `simulation`, `scenario` |

### TODO

Jar + Main 클래스 방식 구성
