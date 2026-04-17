# ADR 011: Atomic Multi-Plan Redis Lua Execution

## Status
Accepted

## Context
As defined in **ADR 004** and **ADR 005**, the rate limiter supports "Chained Limits" where a single request must be checked against multiple plans (e.g., 10/sec and 1000/hour) atomically.
A request must only be allowed, and tokens deducted, if **ALL** limits pass. If even one fails, the entire request must be denied, and **NO** tokens should be deducted from any bucket. This prevents "partial success" where tokens are wasted on a request that is ultimately blocked.

## Decision
We implemented a single, multi-plan atomic Lua script (`acquire_token.lua`) to process all chained limits in a single network round-trip to Redis.

### 1. Input Structure
*   **KEYS**: The script accepts an arbitrary-length array of alternating key pairs: `[bucket_key_1, config_key_1, bucket_key_2, config_key_2, ...]`.
*   **ARGV**: `[requested_tokens]`

### 2. Processing Phases
*   **Time Synchronization**: The script calls `redis.call('TIME')` exactly once at the beginning. This shared timestamp ensures all buckets are evaluated against the exact same millisecond, eliminating logic drift.
*   **Read/Compute Phase**: The script loops through all key pairs. It reads the configurations and bucket states from Redis, and computes the updated token counts purely in memory. It actively tracks `allow_all`, `min_remaining`, and `max_wait_ms`.
*   **Commit Phase (All-or-Nothing)**: Only if `allow_all` is still `true` after evaluating every plan does the script iterate through the temporary memory and execute `HSET` to persist the new token counts to Redis. If any plan denies the request, the script returns immediately without writing anything.

### 3. Fail-Fast Configuration
If any `config_key` is missing in Redis during the loop, the script returns a Redis error immediately. A `RedisConfigBootstrapper` handles pre-loading these configurations so the script never has to wait for Java to supply the rules.

## Consequences
*   **Positive (Performance)**: Minimum possible network overhead. Checking 5 plans takes the same 1 network round-trip as checking 1 plan.
*   **Positive (Correctness)**: Perfect atomicity for chained limits.
*   **Negative**: The Lua script logic is slightly more complex, utilizing temporary Lua tables to hold state between the read and commit phases.