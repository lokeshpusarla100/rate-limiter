# ADR 007: Distributed Time Consistency

## Status
Accepted

## Context
A distributed rate limiter requires a consistent view of time across multiple application instances to correctly calculate token refills. 
The current implementation used `System.nanoTime()`, which is problematic because:
1.  **Arbitrary Origin**: `nanoTime()` origin is JVM-specific and restarts with the process. Values cannot be shared between instances.
2.  **Clock Skew**: Even using `System.currentTimeMillis()`, different servers may have clocks that are seconds apart, leading to unfair rate limiting.

## Decision
We will adopt a multi-tiered time strategy to ensure consistency and precision.

### 1. Source of Truth: Redis Time
For the production Redis-based implementation, the Lua script will call `redis.call('TIME')`.
*   This ensures all application instances use the **exact same clock** (the Redis server's clock).
*   It eliminates clock skew and the need for NTP synchronization between app servers for the sake of rate limiting.

### 2. Unit of Measure: Epoch Microseconds/Milliseconds
*   We will store the `last_refill` timestamp in Redis as an **Epoch-based** value (e.g., milliseconds or microseconds since 1970).
*   **Rationale**: Unlike `nanoTime()`, Epoch time is a universal coordinate. If we ever need to debug the Redis state, the numbers will be human-readable timestamps.

### 3. Precision vs. Accuracy
*   While `redis.call('TIME')` provides microsecond precision, we will evaluate if Milliseconds are sufficient to reduce storage space (though 8-byte Longs fit both).
*   **Recommendation**: Use **Milliseconds** for the `last_refill` field to align with standard Java `System.currentTimeMillis()`, but keep calculation logic in `double` for precision.

### 4. Local Simulation (Core)
*   The Java `TokenBucket` in the Core will use `System.currentTimeMillis()` for its tests.
*   **Constraint**: The `RateLimiterRepository` interface will **not** take a timestamp. The adapter is responsible for fetching time from its specific source of truth (Redis for production, System for memory).

## Consequences
*   **Performance**: Calling `TIME` inside Lua adds a tiny overhead, but it is safer than trusting client clocks.
*   **Consistency**: Guarantees that the rate limiter behaves identically regardless of which app instance handles the request.
