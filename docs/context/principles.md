# Coding Principles & Engineering Standards

This document defines the strict engineering standards and coding principles followed by the `d-rate-limiter` project. These principles ensure that the library is production-grade, reliable, and maintainable.

## 1. The SOLID Foundation
*   **S (Single Responsibility)**: Every class has one job. `TokenBucket` does math; `DefaultRateLimiter` orchestrates; `RedisRepo` does IO.
*   **O (Open/Closed)**: The library is open for extension (custom `KeyResolvers`, `PlanRegistries`, `EventListeners`) but closed for modification.
*   **L (Liskov Substitution)**: Any implementation of a Port (e.g., an In-Memory vs. Redis Repository) is perfectly interchangeable without breaking the system.
*   **I (Interface Segregation)**: We use `default` methods in interfaces (like `RateLimitEventListener`) so clients aren't forced to implement methods they don't need.
*   **D (Dependency Inversion)**: Our **Hexagonal Architecture** ensures the "Brain" (Core) doesn't depend on "Tools" (Redis/Spring). Both depend on abstractions (Ports).

## 2. Architectural Principles
*   **Hexagonal Architecture (Ports & Adapters)**: Clear boundaries between domain logic and infrastructure.
*   **Screaming Architecture**: The package names (`.model`, `.port`, `.service`, `.support`) explicitly tell you what the system does and how it's structured.
*   **Domain-Driven Logic**: We avoid "Anaemic Domain Models." Business logic (refill/consume) lives inside the `TokenBucket` entity, not in a utility class.

## 3. Reliability & Resilience (ADR 002)
*   **Fail-Fast (Inputs)**: We validate everything at the constructor level. If a config is bad, the app fails at startup, not at 3 AM during a request.
*   **Fail-Safe / Fail-Open (Runtime)**: If the database (Redis) dies, the library **must not** block the user's traffic. We log the error and allow the request.
*   **Zero Transitive Dependencies**: The `core` module uses **Pure Java only**. This prevents "Dependency Hell" for the people using our library.

## 4. Logic & Fairness Principles
*   **Distributed Consistency (Source of Truth)**: Redis is the single clock for the whole cluster. We don't trust application server clocks (ADR 007).
*   **Atomicity (All-or-Nothing)**: Chained limits must be atomic. We never deduct tokens from Plan A if Plan B is going to block the request.
*   **Immutability**: We use Java `records`. Objects are side-effect-free. Calculating a refill returns a *new* bucket; it doesn't change the old one.

## 5. Process & Quality Principles
*   **TDD (Red-Green-Refactor)**: We write the test before the code. This ensures 100% coverage and forces us to design better APIs.
*   **KISS (Keep It Simple, Stupid)**: We favor simple, readable code over clever "magic."
*   **DRY (Don't Repeat Yourself)**: We maintain alignment between Java and Lua logic to ensure the "simulation" matches the "production" reality.

## 6. Transparency & Observability (ADR 009)
*   **Semantic Clarity**: We return rich metadata (`RateLimitResult`), not just a boolean, so users know *why* they were blocked.
*   **Event-Driven Monitoring**: We use hooks to allow users to plug in Prometheus or Logging without touching our core code.
