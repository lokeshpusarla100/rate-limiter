# ADR 001: Hexagonal Architecture (Ports & Adapters)

## Status
Accepted

## Context
We are building a distributed rate-limiting library (`d-rate-limiter`) intended for high-throughput production environments.
The core challenge in such a library is coupling:
1.  **Logic vs. Infrastructure**: The mathematical logic of the Token Bucket algorithm (refill rates, capacity checks) is often mixed with the low-level Redis commands (HSET, LUA execution).
2.  **Testability**: Testing rate-limiting logic usually requires spinning up a real Redis instance, making unit tests slow and flaky.
3.  **Future Proofing**: While Redis is the primary backend today, a "Production Grade" library should not be tightly coupled to one vendor. Users might require Hazelcast, Ignite, or even In-Memory implementations for local development.

## Decision
We will adopt the **Hexagonal Architecture** (also known as Ports & Adapters) for the library's design.

### The Structure
1.  **The Hexagon (Core Domain)**:
    *   Contains the **Business Logic**: `TokenBucket` class (Entities), `RateLimiter` interface (Ports).
    *   **Rule**: This layer MUST NOT depend on Spring, Redis, or HTTP. Pure Java only.
    *   **Responsibility**: Decides *if* a request should be allowed based on abstract state.

2.  **The Adapters (Infrastructure)**:
    *   **Driven Adapter (Right Side)**: `RedisRateLimiterRepository`. Implements the repository interface defined in the Core to talk to Redis using Lettuce/Lua.
    *   **Driving Adapter (Left Side)**: `RateLimitAspect` (Spring AOP). Intercepts user requests and "drives" the Core logic.

## Consequences

### Positive (Why we did it)
*   **Testability**: We can write blazing fast Unit Tests for the Token Bucket algorithm by mocking the `Repository` port or using a simple `HashMap` adapter. No Docker required for logic verification.
*   **Isolation**: Changing the underlying Redis client (e.g., from Jedis to Lettuce) or storage format (String to Hash) only touches the Adapter, not the Core logic.
*   **Clarity**: It forces a clear mental model. The "Math" lives in one place, the "IO" in another.

### Negative (The Trade-offs)
*   **Boilerplate**: We need to define Interfaces (Ports) and Data Transfer Objects (DTOs) to cross the boundary between Core and Infrastructure.
*   **Indirection**: Tracing a call requires jumping from the Aspect -> Core Service -> Repository Interface -> Redis Implementation. This can be slightly harder to debug for juniors than a simple script.

## Alternatives Considered
*   **Layered Architecture (Controller -> Service -> Redis)**: Rejected. It encourages leaking Redis specifics (like Connection Pools or Transaction logic) into the Service layer, making the business logic hard to test in isolation.
