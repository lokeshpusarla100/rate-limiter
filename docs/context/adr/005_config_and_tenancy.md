# ADR 005: Configuration & Multi-Tenancy

## Status
Accepted

## Context
Users need to define rate limit rules (Capacity, Refill Rate) and apply them to specific requests.
Hardcoding rules in annotations (`@RateLimit(rps=10)`) is inflexible because changing a limit requires a code deploy.
We also need to support "Chained Limits" (e.g., 10/sec AND 1000/hour) and different "Keys" (IP vs User).

## Decisions

### 1. Hybrid Configuration (Plans)
We will separate **"Rules"** (The limits) from **"Binding"** (Where to apply them).
*   **Rules**: Defined in YAML (Startup) or Redis (Dynamic). We call these "Plans".
    *   Example: `Plan "silver" = 5 req/sec`.
*   **Binding**: Defined in Code (Annotations).
    *   Example: `@RateLimit(plan = "silver")`.

### 2. Dynamic Rules (Redis-Backed)
*   **Decision**: The "Limit" (Capacity/Rate) will be fetched inside the Lua script from a separate Redis Key, falling back to the static config if missing.
*   **Benefit**: This allows **Hot Reloading** of limits. Ops can change the Redis key `config:plan:silver` to `10 req/sec`, and it applies instantly without restarting the app.

### 3. Key Resolvers
We will provide an interface `KeyResolver` to extract the target identity.
*   **Default Implementation**: `IpKeyResolver`, `PrincipalKeyResolver`.
*   **Customization**: Users can implement the interface to extract keys from JWTs, Cookies, or Custom Headers.

### 4. Chained Limits
*   **Decision**: The `@RateLimit` annotation will accept a *list* of plans.
*   **Logic**: The request is checked against ALL plans in the list.
*   **Rule**: The request is allowed IF AND ONLY IF all checks pass. If the first check fails, we short-circuit (do not deduct tokens from subsequent buckets) and block.

## Consequences
*   **Flexibility**: Extremely high. Decouples "Code" from "Policy".
*   **Overhead**: Checking multiple plans (Chained Limits) means multiple Redis calls or a more complex Lua script loop. We will optimize by doing the loop *inside* a single Lua call.
