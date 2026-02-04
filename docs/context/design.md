# Technical Design Document: d-rate-limiter

## 1. System Overview
**d-rate-limiter** is a distributed rate-limiting library for JVM applications. It acts as a middleware that intercepts requests, checks availability against a Token Bucket stored in Redis, and either allows the request to proceed or throws an exception.

### 1.1 Tech Stack
*   **Language**: Java 21
*   **Framework**: Spring Boot 4.0.2
*   **Storage**: Redis (Cluster or Sentinel compatible)
*   **Driver**: Lettuce (Non-blocking I/O)
*   **Format**: Custom Binary Serialization
*   **Build**: Maven Multi-Module

## 2. Component Architecture (Hexagonal)

### 2.1 Component Diagram (Static View)
```mermaid
classDiagram
    class UserApplication {
        +Controller
    }
    class RateLimitStarter {
        +RateLimitAspect
        +AutoConfiguration
    }
    class CoreDomain {
        <<Interface>>
        +RateLimiter
        +TokenBucket
    }
    class RedisAdapter {
        +LettuceConnection
        +LuaScript
    }

    UserApplication --> RateLimitStarter : Uses
    RateLimitStarter --> CoreDomain : Configures
    RateLimitStarter --> RedisAdapter : Wires
    RedisAdapter ..|> CoreDomain : Implements
```

### 2.2 Sequence Diagram (Request Lifecycle)
```mermaid
sequenceDiagram
    participant U as User
    participant A as Aspect (@RateLimit)
    participant S as RateLimiterService
    participant R as RedisAdapter
    participant DB as Redis (Cache)

    U->>A: GET /api/resource
    A->>S: resolveKey(request)
    S-->>A: "user:123"
    A->>S: getRule("gold_plan")
    S-->>A: {cap:10, rate:1.0}
    
    A->>R: allow("user:123", rule)
    
    alt Redis is Healthy
        R->>DB: EVALSHA (sha, key, args)
        DB->>DB: Get Time & Calc Refill
        DB-->>R: 1 (Allowed)
        R-->>A: true
        A->>U: 200 OK (Execute Method)
    else Redis Timeout
        R-->>R: Catch Exception
        R-->>A: true (Fail Open)
        note right of A: Log Error, Don't Block
        A->>U: 200 OK
    end
    
    alt Limit Exceeded
        DB-->>R: 0 (Blocked)
        R-->>A: false
        A-->>U: 429 Too Many Requests
    end
```

### 2.3 Deployment Diagram (Physical View)
```mermaid
graph LR
    subgraph "App Cluster"
        App1[Instance A]
        App2[Instance B]
        App3[Instance C]
    end
    
    subgraph "Infrastructure"
        Redis[(Redis Cluster)]
    end

    App1 -->|Lua Script| Redis
    App2 -->|Lua Script| Redis
    App3 -->|Lua Script| Redis

    style Redis fill:#f9f,stroke:#333
```

### 2.1 Modules
1.  **`d-rate-limiter-core`**:
    *   **Responsibility**: Defines the `TokenBucket` logic and `RateLimiter` interfaces.
    *   **Dependencies**: None (Pure Java).
2.  **`d-rate-limiter-redis`**:
    *   **Responsibility**: Implements `RateLimiterRepository` using Spring Data Redis. Handles Lua scripting, Binary serialization, and connection pooling.
    *   **Dependencies**: `spring-boot-starter-data-redis`.
3.  **`d-rate-limiter-spring-boot-starter`**:
    *   **Responsibility**: Auto-configuration. Detects `application.yml`, registers Beans, and applies the AOP Aspect.
    *   **Dependencies**: `core`, `redis`, `spring-boot-starter-aop`.

## 3. Data Flow (Request Lifecycle)

1.  **Intercept**: Request hits a method annotated with `@RateLimit(key = "user", plan = "gold")`.
2.  **Resolve**:
    *   `KeyResolver` extracts the user ID (e.g., `user:123`).
    *   `PlanRegistry` looks up "gold" config (Capacity: 10, Refill: 1/sec).
3.  **Execute**:
    *   The `RateLimiterService` calls `repository.allow("user:123", 10, 1.0)`.
    *   The Repository calculates the SHA1 of the Lua script.
    *   It executes `EVALSHA` against Redis.
4.  **Redis Logic (Lua)**:
    *   **Get** current `tokens` and `last_refill` from Hash.
    *   **Get** current Server Time (`TIME`).
    *   **Calculate** refill: `delta = (now - last_refill) * rate`.
    *   **Update** tokens: `new_tokens = min(capacity, old_tokens + delta)`.
    *   **Check**: If `new_tokens >= 1`, decrement and return `true`. Else, return `false`.
    *   **Save**: `HSET key ...` (with TTL).
5.  **Result**:
    *   **True**: Method executes.
    *   **False**: Throw `RateLimitExceededException`.

## 4. Data Schema (Redis)

### Key Structure
`rate_limiter:{plan_name}:{resolved_key}`
Example: `rate_limiter:gold:user_123`

### Value Structure (Hash)
| Field | Type | Description |
| :--- | :--- | :--- |
| `t` | Binary (8 bytes) | Current Tokens (Double). |
| `ts` | Binary (8 bytes) | Last Refill Timestamp (Long, nanoseconds). |
| `v` | Integer | Schema Version (Currently `1`). |

*TTL is set to `Capacity / Rate` seconds (bucket lifetime).*

## 5. Class Design (Core)

```java
public interface RateLimiter {
    boolean allow(String key, RateLimitConfig config);
}

public record RateLimitConfig(
    long capacity,
    double tokensPerSecond
) {}

public interface KeyResolver {
    String resolve(HttpServletRequest request);
}
```

## 6. Resilience & Safety
*   **Fail Open**: If `EVALSHA` throws a `RedisConnectionException`, the library logs an error and returns `true` (Allowed).
*   **Self-Healing**: If `EVALSHA` throws `NOSCRIPT`, the library catches it, calls `SCRIPT LOAD`, and retries immediately.
