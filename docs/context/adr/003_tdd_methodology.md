# ADR 003: Test-Driven Development (TDD) Methodology

## Status
Accepted

## Context
We are developing `d-rate-limiter`, a critical infrastructure library.
Errors in this library (e.g., incorrect token calculation, race conditions) will directly impact the availability of consumer applications.
Debugging "logic errors" in a distributed system (Redis + Java) is notoriously difficult and time-consuming.
We need a development methodology that ensures:
1.  **Correctness**: The algorithm behaves exactly as the specification requires.
2.  **Safety**: Refactoring or optimizing code does not secretly break edge cases.
3.  **Design Quality**: The API is intuitive and easy to use for other developers.

## Decision
We will strictly follow **Test-Driven Development (TDD)** for all core logic and infrastructure adapters.

### The Protocol: Red-Green-Refactor

1.  **RED (Write a Failing Test)**:
    *   Before writing any implementation code, we MUST write a unit test that defines the expected behavior.
    *   This test MUST fail initially (either compilation error or assertion error).
    *   *Goal*: Validates that the test is actually testing something and defines the "Done" state for the feature.

2.  **GREEN (Make it Pass)**:
    *   Write the *minimum* amount of code required to pass the test.
    *   Do not over-engineer. Hardcoding return values is acceptable in this phase if it satisfies the test.
    *   *Goal*: Confirm the requirement is met.

3.  **REFACTOR (Clean up)**:
    *   Improve the code structure (SOLID principles, removing duplication, optimizing performance).
    *   Ensure all tests *still* pass.
    *   *Goal*: Maintain code quality without fear of breaking functionality.

### Scope of TDD
*   **Core Domain (`d-rate-limiter-core`)**: Mandatory. All mathematical logic (Token Bucket refill, capacity checks) must be TDD'd.
*   **Redis Adapter (`d-rate-limiter-redis`)**: Mandatory. We will use `Testcontainers` to write integration tests *first* (e.g., "Given Redis is up, when I execute script, then return 1").
*   **Spring Configuration**: Optional. Boilerplate wiring can be integration tested at the end.

## Consequences

### Positive
*   **Confidence**: We will have 100% test coverage on the critical path logic by definition.
*   **Documentation**: The test suite serves as "Living Documentation" of exactly how the library is supposed to behave.
*   **Better API**: Since we write the test (the "Client") first, we naturally design APIs that are easy to consume.

### Negative
*   **Slowness (Perceived)**: Writing tests before code feels slower initially than just "hacking it out". (However, it saves hours of debugging later).
*   **Learning Curve**: Requires discipline to stop and write the test when the solution is already in your head.

## Example Cycle
1.  **Test**: `assert(bucket.refill(time=100) == 5)` -> FAILS.
2.  **Code**: `return 5;` -> PASSES.
3.  **Refactor**: Implement `(time * rate)` formula -> PASSES.
