# ADR 010: Standardized Key Generation

## Status
Accepted

## Context
In a distributed system, key collisions (different users/plans sharing the same Redis key) can lead to incorrect rate limiting or security bypasses. Without a standard format, developers might create inconsistent keys that are hard to debug or monitor.

## Decision
We will provide a standardized `RateLimitKey` builder in the Core support package. [Fix 8]

### 1. Key Format
The standard format is: `ratelimiter:{tenant}:{user}:{plan}`

*   **`tenant`**: Used for multi-tenant SaaS environments (Default: `default`).
*   **`user`**: The unique identifier of the requester (Default: `anonymous`).
*   **`plan`**: The name of the limit plan being applied (Default: `global`).

### 2. Collision Prevention
*   **Namespace isolation**: By including the `tenant` and `plan` in the key, we ensure that the same user ID under different plans or different tenants does not collide.
*   **Builder Pattern**: The `RateLimitKey` builder ensures that developers don't have to manually concatenate strings, reducing the risk of formatting errors.

## Consequences
*   **Pros**: Consistent monitoring, easy debugging in Redis, and guaranteed isolation.
*   **Cons**: Slightly longer keys in Redis (minimal storage impact).
