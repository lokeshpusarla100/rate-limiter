# d-rate-limiter 🛡️

A production-grade, distributed rate-limiting library for Java applications, built with a focus on reliability, performance, and clean architecture.

[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 🚀 Overview

`d-rate-limiter` provides a robust implementation of the **Token Bucket** algorithm for distributed environments. By leveraging Redis and Lua scripting, it ensures atomic operations across multiple application instances while maintaining sub-millisecond overhead.

### Key Features

*   **Distributed Atomicity**: Uses Lua scripts in Redis to prevent race conditions without complex locking.
*   **Hexagonal Architecture**: Core logic is isolated from infrastructure (Redis/Spring), making it highly testable and future-proof.
*   **Fail-Open Resilience**: If Redis becomes unavailable, the library defaults to allowing requests, ensuring your business logic isn't blocked by infrastructure failures.
*   **High Performance**: Custom binary serialization reduces network payload and CPU overhead.
*   **Spring Boot Starter**: Seamless integration with `@RateLimit` annotations and auto-configuration.
*   **Multi-Tenancy**: Support for different "Plans" (e.g., Bronze, Silver, Gold) and flexible "Key Resolvers" (IP, User ID, API Key).

## 🏗️ Architecture

The project follows **Hexagonal Architecture (Ports & Adapters)** principles:

1.  **Core Module**: Pure Java logic. Contains the mathematical Token Bucket implementation and Port definitions.
2.  **Redis Module**: Implementation of the storage adapter using Lettuce and custom Lua scripts.
3.  **Spring Boot Starter**: The "Driving Adapter" that provides AOP aspects and configuration wiring.

Detailed architectural decisions can be found in the [docs/context/adr](./docs/context/adr) directory.

## 🛠️ Tech Stack

*   **Runtime**: Java 21 (Records, Virtual Threads ready)
*   **Framework**: Spring Boot 4.0.2
*   **Communication**: Lettuce (Redis client)
*   **Testing**: JUnit 5, Testcontainers (Redis), AssertJ
*   **Build**: Maven Multi-Module

## 🚦 Getting Started

*(Note: This library is currently in active development. Usage instructions will be updated as Epic 3 is completed.)*

### Prerequisites
*   JDK 21
*   Redis 6.2+

### Installation
Add the dependency to your `pom.xml`:
```xml
<dependency>
    <groupId>com.lokesh.ratelimiter</groupId>
    <artifactId>d-rate-limiter-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## 📜 Principles & Standards

This project is built with high engineering standards:
*   **SOLID**: Strict adherence to object-oriented design principles.
*   **TDD-First**: Every core feature is developed using the Red-Green-Refactor cycle.
*   **Zero Transitive Dependencies**: Minimal external footprint to avoid dependency hell for consumers.
*   **Immutability**: Domain objects are immutable records for thread-safe operations.

## 🗺️ Roadmap

- [x] Epic 1: Project Skeleton & Core Domain (In Progress)
- [ ] Epic 2: Redis Adapter & Lua Scripting
- [ ] Epic 3: Spring Boot Starter & AOP
- [ ] Epic 4: Resilience (Fail-Open) & Metrics
- [ ] Epic 5: Load Testing & Samples

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
