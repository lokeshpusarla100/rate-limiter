# Implementation Roadmap: d-rate-limiter

This document outlines the phased build process for the `d-rate-limiter` library, organized into Epics and Tasks.

## 🚀 Epic 1: Project Skeleton & Core Domain
**Goal**: Establish the multi-module structure and implement the "Pure Java" logic (The Hexagon).

*   **1.1 Project Initialization**: Create root `pom.xml` and child module directories (`core`, `redis`, `starter`).
*   **1.2 Domain Entities**: Define the `TokenBucket` state record and `RateLimitConfig`.
*   **1.3 Outbound Ports**: Define the `RateLimiterRepository` interface (the "Port" to the infrastructure).
*   **1.4 Core Logic (TDD)**: Implement the check/refill logic in Java (for the in-memory fallback/testing).
*   **1.5 Verification**: 100% unit test coverage on the math logic.

## 🚀 Epic 2: The Redis Adapter (The "Infra" Phase)
**Goal**: Implement the distributed logic using Redis and Lua.

*   **2.1 Module Setup**: Configure `d-rate-limiter-redis` with `spring-boot-starter-data-redis`.
*   **2.2 Lua Scripting**: Write the `token_bucket.lua` script (Atomic Get-Calculate-Update).
*   **2.3 Repository Implementation**: Implement `RedisRateLimiterRepository`.
*   **2.4 Binary Serialization**: Implement `Double` and `Long` binary encoders for Redis Hash fields.
*   **2.5 Integration Testing (TDD)**: Setup `Testcontainers` (Redis) to verify script atomicity and self-healing (NOSCRIPT).

## 🚀 Epic 3: Spring Boot Starter (The "Glue" Phase)
**Goal**: Provide a seamless "Auto-Config" experience for consumers.

*   **3.1 Annotation Design**: Create `@RateLimit` and `@RateLimitGroup` for chained limits.
*   **3.2 AOP Aspect**: Create `RateLimitAspect` to intercept annotated methods.
*   **3.3 Key Resolution**: Implement `KeyResolver` interface and standard strategies (IP, Principal).
*   **3.4 AutoConfiguration**: Write the `RateLimiterAutoConfiguration` to wire everything when the library is added to a classpath.

## 🚀 Epic 4: Resilience & Observability
**Goal**: Ensure the library is "Production Ready" regarding failure and monitoring.

*   **4.1 Fail-Safe Implementation**: Implement the "Fail-Open" logic for Redis timeouts/connection errors.
*   **4.2 Metrics Exposure**: Integrate Micrometer to emit `ratelimiter.requests` counters.
*   **4.3 Dynamic Config**: Implement the lookup mechanism for limits stored in Redis (Hot Reload).

## 🚀 Epic 5: Validation & Samples
**Goal**: Demonstration and final polish.

*   **5.1 Sample Application**: Create a Spring Boot app that uses the library to limit a dummy REST API.
*   **5.2 Load Testing**: Use a tool (e.g., k6 or JMeter) to verify the rate limiter holds up under pressure.
*   **5.3 Documentation**: Finalize `README.md` with usage instructions and architecture diagrams.
