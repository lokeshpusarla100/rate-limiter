# ADR 008: Resilience & Circuit Breaker

## Status
Accepted

## Context
Infrastructure (Redis) failures are inevitable. While we have a "Fail-Open" policy in the Core, slamming a failing Redis instance with millions of requests during an outage can cause:
1.  **Log Spam**: Millions of identical error logs.
2.  **Thread Exhaustion**: Application threads waiting for Redis timeouts.
3.  **Cascading Failures**: Stressing the network or Redis further as it tries to recover.

## Decision
We will implement a **Circuit Breaker** pattern using **Resilience4j** to wrap the `RateLimiterRepository`.

### 1. Configuration (Production Defaults)
*   **Sliding Window**: 10 calls (Count-based).
*   **Failure Threshold**: 50%.
*   **Wait Duration (Open)**: 5 seconds (Cool-down period before retrying Redis).
*   **Timeout**: 100ms SLA for Redis commands.

### 2. Behavior
*   When the circuit is **OPEN**, the repository wrapper will immediately return a `RateLimitResult.failOpen("Circuit Breaker Open")` without calling Redis.
*   The state transition (Closed -> Open) will be logged as a critical event.

### 3. Implementation
The Circuit Breaker will be implemented as a **Decorator** in the `d-rate-limiter-redis` or `d-rate-limiter-spring-boot-starter` module, keeping the Core pure.

## Consequences
*   **Pros**: Prevents resource exhaustion, reduces log noise, and provides a clear signal of infrastructure health.
*   **Cons**: Requires a dependency on `resilience4j-circuitbreaker`.
