# Implementation Roadmap: d-rate-limiter

This document outlines the phased build process for the `d-rate-limiter` library, organized into Epics and Tasks.

## 🛠 Methodology: TDD-First
As per **ADR 003**, we strictly follow the **Red-Green-Refactor** cycle:
1.  **Red**: Write a failing test (or one that doesn't compile).
2.  **Green**: Implement minimum code to pass.
3.  **Refactor**: Clean up while maintaining green tests.

## 📝 Critical Engineering Constraints
*To be verified before every commit:*

### 1. Hexagonal Integrity (ADR 001)
- [ ] **Core Purity**: No dependencies on Spring, Redis, or Servlet API in `d-rate-limiter-core`.
- [ ] **Port-Driven**: All IO or external logic must be defined via interfaces (Ports) in the Core.

### 2. Resilience & Safety (ADR 002)
- [ ] **Fail-Fast (Inputs)**: Config and entities must validate inputs at construction (e.g., non-negative capacity).
- [ ] **Fail-Open (Runtime)**: Infrastructure failures (Redis down) must not block the user. Return `ALLOW` and log the error.
- [ ] **Immutability**: Use Java `records` for domain entities to ensure thread safety.

### 3. Multi-Tenancy & Performance (ADR 004/005)
- [ ] **Plan-Based**: Decouple rules from code using the `PlanRegistry`.
- [ ] **Identity Abstraction**: Use `RequestSource` and `KeyResolver` to avoid leaking HTTP into Core.
- [ ] **Binary-First**: Use custom binary serialization for Redis storage (8-byte Double/Long) for performance.

### 4. Logging & Observability
- [ ] **Silent Core**: Core logic should not log to console directly; use SLF4J.
- [ ] **Resilience Logging**: Log every "Fail-Open" event as an ERROR with stack trace.

## 🚀 Epic 1: Project Skeleton & Core Domain [COMPLETED]
**Goal**: Establish the multi-module structure and implement the "Pure Java" logic (The Hexagon).

### 🏆 Completion Report: Epic 1

#### 1. What was built?
We have established the **Core Domain** of the library. This is the "Brain" that defines how rate limiting works without knowing about the outside world (Redis, Spring, HTTP).

#### 2. Component Breakdown (The "How, Why, Where")

| Component | Where? | What is it? | Why did we do it this way? (The Senior "Why") |
| :--- | :--- | :--- | :--- |
| **TokenBucket** | `core` | **State Entity** | Separates the *volatile state* (tokens, time) from the *fixed rules*. It is an immutable record for thread safety. |
| **RateLimitConfig** | `core` | **Policy Entity** | Holds the *Rules* (Capacity, Rate). Validates inputs at startup (Fail-Fast) to prevent invalid config in production. |
| **RateLimiter (Port)** | `core` | **Inbound Interface** | The main entry point. The Aspect will call this. It defines *What* the library does for the user. |
| **RateLimiterRepository (Port)** | `core` | **Outbound Interface** | The "Socket." It defines *What* storage needs the Core has. It **is not** the implementation. |
| **DefaultRateLimiter** | `core` | **Domain Service** | The "Orchestrator." It contains the **Fail-Open logic**. If storage crashes, it protects the user by allowing traffic. |
| **RequestSource** | `core` | **Abstraction** | A "Translator." It allows the Core to ask for an IP or Header without needing to import heavy Web libraries. |

#### 3. The "Pure Java" Flow (How a request moves)
1.  **Request** enters the system via the **Adapter** (e.g., Spring AOP).
2.  **Adapter** uses the `KeyResolver` and `PlanRegistry` (Ports) to gather identity and rules.
3.  **Adapter** calls the `DefaultRateLimiter` (Service).
4.  **Service** tells the `RateLimiterRepository` (Port): *"Try to acquire a token for this key."*
5.  **Service** handles the result:
    - If storage says "OK" -> Return `true`.
    - If storage says "Exceeded" -> Return `false`.
    - If storage **Crashes** (e.g., Redis down) -> Service catches it, logs an ERROR, and returns `true` (**Fail-Open**).

#### 4. The Hexagonal Distinction
- **Logic (Core)**: Knows *What* should happen (e.g., "If storage fails, allow request").
- **Infrastructure (Redis/Starter)**: Knows *How* it happens (e.g., "Connect to Redis via Lettuce").
By keeping the interface in the Core, we can test the entire "Fail-Open" logic without even having Redis installed.

---

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
