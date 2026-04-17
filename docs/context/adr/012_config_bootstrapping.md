# ADR 012: Configuration Synchronization (Bootstrapping)

## Status
Accepted

## Context
In **ADR 005**, we decided on a "Hybrid Configuration" where rate-limit plans are defined in Java/YAML but fetched from Redis inside the Lua script to support "Hot Reloading." 
However, this creates a synchronization gap: if a plan is updated in the application's `application.yml`, the Redis copy remains stale or might be missing entirely if Redis is flushed. The Lua script requires these rules (capacity, refill rate) to exist in Redis Hashes to function.

## Decision
We implemented a **Redis Config Bootstrapper** to synchronize the Java `PlanRegistry` with Redis storage during application startup.

### 1. Synchronization Flow
1.  **Startup**: The application reads local configuration (e.g., YAML) into the Core `PlanRegistry`.
2.  **Bootstrap**: A `RedisConfigBootstrapper` iterates through all registered `RateLimitConfig` objects.
3.  **Persistence**: For each plan, the bootstrapper executes an `HSET` command to store the `capacity` and `refillRate` in a standard Redis key: `config:plan:{planName}`.

### 2. Strategic Rationale
*   **Decoupling**: The Redis adapter remains a "dumb pipe." It doesn't know about Spring or YAML; it only knows how to move Java objects into Redis.
*   **Operational Agility**: By pre-loading Redis, we ensure the "Source of Truth" for the Lua script is always the local Redis server, which is significantly faster than calling back to the application.
*   **Hot-Reloading Ready**: This baseline synchronization ensures that if an Ops team manually updates a Redis Hash during a traffic spike, the Lua script will use the new value immediately, while the Bootstrapper ensures the app can always restore the "known good" baseline on restart.

## Consequences
*   **Positive**: Guaranteed consistency between application rules and Redis-native logic.
*   **Positive**: Simplified Lua script logic (it only reads from one source: Redis).
*   **Negative**: Slight increase in startup time proportional to the number of rate-limit plans (negligible for most use cases).
