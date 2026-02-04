# ADR 002: Coding Principles & Standards

## Status
Accepted

## Context
We are building a **Production-Grade Library** (`d-rate-limiter`) that will be embedded into other applications.
Unlike a standalone microservice, a library has strict constraints:
1.  **Trust**: If the library crashes, the user's application crashes. Reliability is non-negotiable.
2.  **Compatibility**: We cannot force heavy dependencies (like Guava or Resilience4j) onto the user.
3.  **Readability**: As an open-source/resume project, the code serves as documentation of the author's skill.

## Decision
We will adhere to a strict set of Engineering Principles tailored for **High-Reliability Library Design**.

### 1. SOLID Principles (The Foundation)
*   **Single Responsibility Principle (SRP)**:
    *   *Where*: Every class must have one reason to change.
    *   *How*: `TokenBucket` handles math. `RedisRateLimiter` handles IO. `RateLimitAspect` handles interception. We strictly avoid "God Classes" that do all three.
*   **Open/Closed Principle (OCP)**:
    *   *Where*: Configuration and Strategy selection.
    *   *How*: We use Interfaces (`KeyResolver`, `RefillStrategy`). Users can implement their own `KeyResolver` without modifying our library code.
*   **Dependency Inversion Principle (DIP)**:
    *   *Where*: Core Domain vs Infrastructure.
    *   *How*: High-level modules (Core Logic) will depend on abstractions (`RateLimiterRepository`), not concrete details (`LettuceConnection`).

### 2. Defensive Programming (The Safety Net)
*   **Fail Fast (Inputs)**:
    *   *Why*: Invalid configuration should blow up at **Startup**, not at Runtime.
    *   *How*: All constructors invoke `Objects.requireNonNull` or `Assert.isTrue`. No "null" implies "default" magic in the core domain.
*   **Fail Safe (Runtime)**:
    *   *Why*: Infrastructure (Redis) *will* fail.
    *   *How*: Critical IO paths must have `try-catch` blocks. If Redis times out, the library defaults to `ALLOW` (Fail Open) and logs the error, ensuring we never block the user's business traffic due to our infra issues.

### 3. Library Specific Constraints
*   **Zero Transitive Dependencies**:
    *   *Why*: Avoid "Dependency Hell" for the consumer.
    *   *How*: We will use standard Java features (Records, Optional) instead of utility libraries (Lombok, Apache Commons) wherever possible.
*   **Immutability**:
    *   *Why*: Thread safety is critical in high-concurrency environments.
    *   *How*: Domain objects (`TokenBucket`) will be immutable `records`. State changes result in *new* objects, not mutated fields (functional style).

## Consequences

### Positive
*   **Robustness**: Defensive coding prevents "NullPointerException" surprises in production.
*   **Maintainability**: SRP makes it easy to locate bugs. If the math is wrong, check `Core`. If Redis fails, check `Infra`.
*   **Professionalism**: Demonstrates "Senior" mindset—optimizing for the *consumer's* stability, not the *author's* convenience.

### Negative
*   **Verbosity**: Writing `Objects.requireNonNull` and Interface wrappers adds lines of code compared to "Quick and Dirty" scripting.
*   **Development Time**: Thinking through OCP requires more upfront design than just writing an `if/else` block.
