import http from "k6/http";
import { sleep, check } from "k6";

export const options = {
  scenarios: {
    smoke: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "10s", target: 10 },
        { duration: "20s", target: 10 },
        { duration: "10s", target: 0 },
      ],
      gracefulRampDown: "5s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],          // 에러율 1% 미만
    http_req_duration: ["p(95)<300"],        // p95 300ms 미만
  },
};

export default function () {
  const res = http.get("https://httpbin.test.k6.io/get");

  check(res, {
    "status is 200": (r) => r.status === 200,
    "ttfb < 200ms": (r) => r.timings.waiting < 200,
  });

  sleep(1);
}