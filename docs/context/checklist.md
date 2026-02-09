# Senior Engineer's Pre-Flight Checklist

*This checklist must be reviewed before every implementation turn to ensure alignment with project principles and the 12 Hardening Fixes.*

## 1. TDD Protocol (ADR 003)
- [ ] **RED**: Write a failing test first. No "implementation-first" shortcuts.
- [ ] **GREEN**: Write the minimum code to pass.
- [ ] **REFACTOR**: Clean code while maintaining green tests.
- [ ] **Compatibility [Fix 1]**: For infrastructure (Lua), write a Java-Lua compatibility test suite.

## 2. Architectural Boundaries (ADR 001)
- [x] **Core Purity**: No Spring, Redis, or Web dependencies in `d-rate-limiter-core`.
- [x] **Hexagonal Flow**: Is the boundary between Port and Adapter respected?

## 3. Resilience & Security (ADR 002, 008)
- [x] **Fail-Fast**: Are inputs validated at the earliest possible moment?
- [x] **Fail-Open**: Does IO failure allow traffic instead of blocking?
- [ ] **Circuit Breaker [Fix 3]**: Is the Redis repository wrapped in a Resilience4j circuit breaker?
- [x] **Plan Security [Fix 2]**: Is the `MissingPlanPolicy` enforced?

## 4. Logic & Data Integrity (ADR 004, 007, 010)
- [x] **Domain Math [Fix 6]**: Does the consumption logic live in `TokenBucket.tryConsume`?
- [x] **Source of Truth**: Is Redis Server Time used for distributed refills?
- [x] **Standard Keys [Fix 8]**: Does the key follow `ratelimiter:{tenant}:{user}:{plan}`?
- [x] **Binary-First**: Is custom binary serialization used for Redis storage?

## 5. Observability (ADR 009)
- [x] **Event Hooks [Fix 7]**: Does the service notify `RateLimitEventListener`?
- [x] **Semantic Clarity [Fix 4]**: Does the result return the *minimum* tokens across chained limits?

## 6. Code Quality (ADR 002, 012)
- [x] **Immutability**: Are all domain models Java `records`?
- [x] **Thread Safety [Fix 12]**: Is the concurrency contract documented and implemented?
- [x] **Precision [Fix 9]**: Are floating-point drift risks documented/tested?

---
*Reference: See `docs/context/adr/` and `docs/context/principles.md` for detailed rationale behind these checks.*
