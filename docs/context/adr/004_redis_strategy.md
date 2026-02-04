# ADR 004: Redis Data Strategy & Lua Execution

## Status
Accepted

## Context
We are implementing a Distributed Token Bucket algorithm. The shared state (tokens, time) must be stored in Redis to be accessible by multiple application instances.
Performance requirements are strict (< 5ms), and correctness (atomicity) is paramount.

## Decisions

### 1. Data Structure: Redis Hash (`HSET`)
We will store the bucket state in a Redis Hash.
*   **Key**: `rate_limiter:{plan_name}:{target_key}` (e.g., `rate_limiter:premium:user_123`)
*   **Fields**:
    *   `tokens` (Binary): The current token count (Double precision).
    *   `last_refill` (Binary): The timestamp of the last refill (Long, nano/milliseconds).
    *   `v` (Integer): Schema version (for backward compatibility).

### 2. Serialization: Binary (Custom)
*   **Decision**: We will use custom binary serialization (`ByteBuffer`) for the values.
*   **Rationale**: 
    *   Optimization: Reduces network payload size compared to ASCII strings.
    *   Accuracy: Avoids floating-point parsing errors that can happen with String conversion.
*   **Mitigation**: To prevent deserialization crashes, we include a `v` (version) field. If the version doesn't match the library version, the bucket is discarded/reset.

### 3. Execution: Lua with `EVALSHA` & Self-Healing
*   **Problem**: Sending the full Lua script on every request consumes bandwidth.
*   **Decision**: Use `EVALSHA`. The client sends the SHA1 hash of the script.
*   **Resilience**: We will implement a **Self-Healing Mechanism**.
    *   Catch `NOSCRIPT` error from Redis.
    *   On error, automatically run `SCRIPT LOAD` to re-upload the script.
    *   Retry the execution.
    *   This ensures the system recovers if Redis is restarted or flushed.

### 4. Time Source: Redis Time
*   **Decision**: The Lua script will call `redis.call('TIME')` to get the current time.
*   **Rationale**: Eliminates "Clock Skew" issues between different application servers. The Redis server becomes the single source of truth for time.

## Consequences
*   **Complexity**: Binary serialization requires writing custom Encoders/Decoders. Debugging requires a custom tool to read the binary values.
*   **Performance**: `EVALSHA` + Binary is the highest performance option available.
