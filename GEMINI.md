# d-rate-limiter Project Context

## Overview
A production-grade, distributed rate-limiting library for Java applications, using Redis for state and adhering to Hexagonal Architecture.

## Architecture
- **Style**: Hexagonal (Ports & Adapters).
- **Core**: Pure Java, zero heavy dependencies.
- **Infrastructure**: Redis (Lettuce), Spring Boot Starter.
- **Key Patterns**: Fail-Open, Atomic Chained Limits, Distributed Time Consistency.

## Current Status
- **Epic 1 (Core Domain)**: Complete.
- **Epic 1.5 (Refinement)**: Complete.
- **Epic 2 (Redis Adapter)**: **IN PROGRESS**. Focus on `d-rate-limiter-redis` module, Lua scripting, and binary serialization.

## Key Principles
1.  **Fail-Open**: Never block traffic due to infrastructure failure.
2.  **Hexagonal**: Core depends on nothing but itself (and SLF4J).
3.  **Accuracy**: Use Redis Server Time for clock skew protection.
4.  **TDD**: Write tests first.

## Module Structure
- `d-rate-limiter-core`: Domain logic (TokenBucket, DefaultRateLimiter).
- `d-rate-limiter-redis`: Redis implementation of RateLimiterRepository.
- `d-rate-limiter-spring-boot-starter`: Auto-configuration.

## Quick References
- **Design**: `docs/context/design.md`
- **Principles**: `docs/context/principles.md`
- **ADRs**: `docs/context/adr/`
