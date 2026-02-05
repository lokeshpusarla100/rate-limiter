# ADR 009: Observability & Event Hooks

## Status
Accepted

## Context
Production environments require visibility into rate-limiting events (e.g., how many users are hitting limits, how often is the system failing-open). 
Hardcoding Prometheus or SLF4J logic into the Core violates **ADR 001 (Hexagonal Architecture)**.

## Decision
We will introduce an **Outbound Event Port** (`RateLimitEventListener`) in the Core.

### 1. The Interface
```java
public interface RateLimitEventListener {
    void onAllow(String key, List<String> plans, RateLimitResult result);
    void onDeny(String key, List<String> plans, RateLimitResult result);
    void onFailOpen(String key, String reason);
    void onPlanMissing(String planName);
}
```

### 2. Orchestration
The `DefaultRateLimiter` service will accept a `List<RateLimitEventListener>` and notify them of every relevant event.

### 3. Standard Adapters
We will provide:
1.  **`MicrometerEventListener`**: Publishes counters and timers to Prometheus/Grafana.
2.  **`LoggingEventListener`**: Produces structured logs for ELK/Splunk.
3.  **`NoOpEventListener`**: Default implementation with zero overhead.

## Consequences
*   **Pros**: Enables deep observability without coupling the Core to specific monitoring tools.
*   **Cons**: Introduces a small overhead for event notification (minimized by using No-Op default).
