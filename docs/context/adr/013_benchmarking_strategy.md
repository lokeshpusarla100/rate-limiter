# ADR 013: Performance Verification & Load Testing Strategy

## Status
Accepted

## Context
A distributed rate limiter is a "high-pressure" component. If it is slow, the entire application slows down. To claim that the library is "Production-Ready" on a resume or in an interview, we must have empirical evidence of its performance characteristics under concurrent load. 

## Decision
We adopted a multi-stage benchmarking strategy using `k6` and `curl` to verify the system's limits and latency.

### 1. Benchmarking Targets
*   **Throughput**: Verify the system can handle bursts of requests significantly higher than the rate limit without leaking tokens or crashing.
*   **Latency (p99)**: Ensure the "Round-trip" (App -> Redis -> Lua -> App) stays below a strict 10ms threshold in a local environment.
*   **Correctness**: Verify that exactly `N` requests are allowed when the capacity is `N`, and all subsequent requests are rejected with a 429.

### 2. Tools & Methodology
*   **k6**: Chosen for its high-concurrency capabilities and ability to define automated thresholds (e.g., "fail if p99 > 50ms").
*   **Sample Endpoint**: We built a dedicated `/limited` endpoint that mirrors a real-world production controller, allowing us to measure the full overhead of the library, including the Java/Redis bridge.
*   **Stress Testing**: We manually executed 150 concurrent requests against a 100-token bucket to verify script atomicity and the "All-or-Nothing" commit phase in the Lua script.

## Consequences
*   **Positive**: Provides "Hard Numbers" (e.g., RPS and p99) for resume and technical discussions.
*   **Positive**: Proves the correctness of the distributed algorithm in a way that unit tests cannot.
*   **Positive**: Established a repeatable baseline for future optimizations (like binary serialization or circuit breaking).
