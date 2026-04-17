# ADR 014: Choice of Redis Client (Lettuce)

## Status
Accepted

## Context
A distributed rate limiter must handle a high volume of concurrent network requests to the Redis server (sustained 10,000+ RPS as verified in our load tests). We need a Java Redis client that is thread-safe, provides high performance, and is compatible with the Spring ecosystem.

## Decision
We chose **Lettuce** as the primary Redis client for the `d-rate-limiter-redis` module.

### Rationale
1.  **Non-Blocking I/O**: Lettuce is built on top of **Netty**, a high-performance asynchronous networking framework. This allows it to handle many concurrent connections more efficiently than traditional "one-connection-per-thread" clients like Jedis.
2.  **Thread Safety**: A single Lettuce connection is thread-safe and can be shared among multiple threads. This reduces the overhead of connection pooling and management.
3.  **Modern Feature Support**: Lettuce has native support for advanced Redis features like `EVALSHA`, Cluster mode, and Sentinel, which are critical for our distributed architecture.
4.  **Spring Ecosystem Alignment**: Lettuce is the default Redis driver for Spring Data Redis, ensuring our `redis` module will be easy to integrate when we build the Spring Boot Starter in Epic 3.

## Consequences
*   **Positive**: Proven capability to sustain 10k+ RPS with sub-10ms p99 latency in a local environment.
*   **Positive**: Reduced memory footprint due to efficient connection sharing.
*   **Negative**: Netty-based stack can be slightly more complex to debug for developers only familiar with standard synchronous blocking I/O.
