/**
 * k6 Load Test Script
 * 
 * Target: /limited endpoint
 * Goal: Verify p99 latency stays under 50ms while hitting the rate limit.
 */
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 50 }, // Warm-up to 50 virtual users
    { duration: '30s', target: 50 }, // Steady state for 30 seconds
    { duration: '10s', target: 0 },  // Ramp down to 0
  ],
  thresholds: {
    // We expect failures (429s) because we are hitting it with 50 concurrent users 
    // but the limit is only 100 requests per second.
    // The important metric is that the p99 latency stays very low.
    http_req_duration: ['p(99)<50'], // 99% of requests should be below 50ms
  },
};

export default function () {
  // Hit our sample endpoint
  const res = http.get('http://localhost:8080/limited');
  
  // Verify we only get 200 (Success) or 429 (Rate Limited)
  check(res, {
    'is status 200 or 429': (r) => r.status === 200 || r.status === 429,
    'is not 500 (Fail Open/Error)': (r) => r.status !== 500,
  });
}