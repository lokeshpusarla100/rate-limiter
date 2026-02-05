# d-rate-limiter 🛡️

A production-grade, distributed rate-limiting library for Java applications, built with a focus on reliability, performance, and clean architecture.

[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Architecture](https://img.shields.io/badge/Architecture-Hexagonal-orange.svg)](#-architecture)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 🚀 Overview

`d-rate-limiter` provides a robust, high-performance implementation of the **Token Bucket** algorithm for distributed JVM environments. It is designed to be a "Zero-Friction" library that protects your services without introducing fragility.

### Key Features (Post-Epic 1.5 Refinement)

*   **🛡️ Fail-Open Resilience**: Infrastructure (Redis) failures log errors but never block traffic. Your business remains available even if the rate limiter is down.
*   **🔗 Atomic Chained Limits**: Apply multiple limits (e.g., 10/sec AND 1000/hour) in a single atomic operation. No partial token leaks.
*   **📊 Rich Metadata**: Returns `RateLimitResult` containing remaining tokens and `waitMillis` for helpful `Retry-After` headers.
*   **⚖️ Request Weighting**: Support for "Expensive" vs "Cheap" operations (e.g., Bulk Export costs 5 tokens, Ping costs 1).
*   **🕒 Distributed Consistency**: Uses Redis Server Time as the source of truth to eliminate clock skew between application instances.
*   **🧩 Hexagonal Integrity**: Core logic is 100% "Pure Java" with zero external dependencies, ensuring maximum portability and testability.

## 🏗️ Architecture

The project follows a strict **Hexagonal Architecture (Ports & Adapters)** with a "Screaming" package structure:

*   **`com.lokesh.ratelimiter.core.model`**: Immutable domain entities (TokenBucket, Config).
*   **`com.lokesh.ratelimiter.core.port`**: Driving and Driven interfaces defining the system's boundaries.
*   **`com.lokesh.ratelimiter.core.service`**: Domain orchestrators (DefaultRateLimiter) implementing Fail-Open and Chained logic.
*   **`com.lokesh.ratelimiter.core.support`**: Standard implementations (Key Resolvers, Registries) to reduce developer friction.

## 🛠️ Tech Stack

*   **Runtime**: Java 21 (Records, Sealed Types)
*   **Framework**: Spring Boot 4.0.2
*   **Communication**: Lettuce (Non-blocking Redis client)
*   **Serialization**: Custom Binary (ADR 004) for ultra-low latency.
*   **Testing**: JUnit 5, Mockito, AssertJ, Testcontainers.

## 🚦 Usage

### 1. Define Plans
```java
InMemoryPlanRegistry registry = new InMemoryPlanRegistry();
registry.registerPlan(new RateLimitConfig("gold", 100, 10.0)); // 100 capacity, 10 tokens/sec
```

### 2. Execute Check
```java
RateLimitResult result = rateLimiter.allow("user_123", List.of("gold"), 1);

if (result.allowed()) {
    // Process request...
    System.out.println("Remaining: " + result.remainingTokens());
} else {
    // Block request...
    System.out.println("Retry after: " + result.waitMillis() + "ms");
}
```

## 📜 Architectural Decisions (ADRs) & Principles

We maintain a disciplined log of architectural choices and standards:
- [ADR 001: Hexagonal Architecture](./docs/context/adr/001_architecture_pattern.md)
- [ADR 004: Redis Data Strategy & Binary Serialization](./docs/context/adr/004_redis_strategy.md)
- [ADR 005: Atomic Chained Limits & Configuration](./docs/context/adr/005_config_and_tenancy.md)
- [ADR 007: Distributed Time Consistency](./docs/context/adr/007_distributed_time_consistency.md)
- [Coding Principles & Standards](./docs/context/principles.md)

## 🗺️ Roadmap

- [x] **Epic 1**: Core Domain Implementation
- [x] **Epic 1.5**: Structural Refinement (10 Gaps Resolved)
- [ ] **Epic 2**: Redis Adapter & Lua Scripting (Current)
- [ ] **Epic 3**: Spring Boot Starter & AOP
- [ ] **Epic 4**: Resilience & Observability
- [ ] **Epic 5**: Load Testing & Samples

## 📄 License

MIT © [Lokesh](LICENSE)