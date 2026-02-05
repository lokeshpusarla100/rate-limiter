# ADR 006: Dual-Implementation Strategy (Java vs. Lua)

## Status
Accepted

## Context
Following **ADR 001 (Hexagonal Architecture)**, we maintain a pure Java implementation of the Token Bucket algorithm in the Core module for testing and local simulations. 
However, **ADR 004** requires a Lua implementation in Redis for production use.
This creates a "Shadow Logic" problem where the same mathematical algorithm is implemented in two different languages (Java and Lua), risking logic drift.

## Decision
We will treat the **Lua Implementation** as the "Source of Truth" for production behavior, while maintaining the **Java Implementation** as a "Verified Simulation".

### Strategy for Alignment:
1.  **Strict Algorithm Mapping**: The Lua script must follow the exact same mathematical steps as the `TokenBucket.refill` method (Refill -> Cap -> Compare -> Deduct).
2.  **Cross-Language Verification Tests**: We will implement integration tests that:
    *   Generate a suite of test cases (Initial Tokens, Rate, Capacity, Time Elapsed, Requested Tokens).
    *   Run these cases through the `TokenBucket` Java class.
    *   Run the *same* cases through the Redis Lua script (using Testcontainers).
    *   Assert that the results (Allowed/Denied, Remaining Tokens) are identical to the last decimal place.
3.  **Core for Local/Unit**: The Java implementation remains in the Core to allow developers to run 100% of their business logic tests without needing a Redis instance.

## Consequences

### Positive
*   **Testability**: Unit tests remain fast and dependency-free.
*   **Confidence**: Cross-verification ensures that what we test locally is what runs in production.

### Negative
*   **Maintenance**: Any change to the algorithm (e.g., adding a "burst" multiplier) must be implemented twice and verified.
