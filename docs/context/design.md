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

## 2. Architecture (Hexagonal / Ports & Adapters)

The system strictly adheres to the **Hexagonal Architecture**. This ensures that the core business logic (The "Domain") is completely isolated from external concerns like the Web Framework (Spring) or the Database (Redis).

### 2.1 The Hexagon (Detailed View)

This diagram illustrates the separation between the **Core Domain** (Blue), the **Ports** (Yellow), and the **Infrastructure Adapters** (Green).

```mermaid
graph TD
    subgraph "External World"
        User[User / Client]
        RedisDB[(Redis Cluster)]
    end

    subgraph "Adapters (Infrastructure Layer)"
        direction TB
        subgraph "Driving Adapter (Spring Starter)"
            Aspect[RateLimitAspect]
            WebAdapter[ServletRequestAdapter]
        end
        subgraph "Driven Adapter (Redis Module)"
            RedisRepo[RedisRateLimiterRepository]
            Lua[Lua Script]
        end
    end

    subgraph "d-rate-limiter-core (The Hexagon)"
        direction TB
        
        subgraph "Inbound Ports (API)"
            RateLimiterPort((RateLimiter))
            RegistryPort((PlanRegistry))
            KeyPort((KeyResolver))
        end

        subgraph "Domain Layer (Pure Java)"
            Service[DefaultRateLimiter]
            
            subgraph "Entities"
                Bucket[TokenBucket]
                Config[RateLimitConfig]
                Result[RateLimitResult]
            end
        end
        
        subgraph "Outbound Ports (SPI)"
            RepoPort((RateLimiterRepository))
            SourcePort((RequestSource))
        end

        subgraph "Support (Standard Impls)"
            MemRegistry[InMemoryPlanRegistry]
            HeaderKey[HeaderKeyResolver]
        end
    end

    %% --- Relationships ---

    %% Driving Flow
    User -->|HTTP Request| Aspect
    Aspect -->|1. Resolve Key| KeyPort
    Aspect -->|2. Check Limit| RateLimiterPort
    
    %% Core Orchestration
    Service -- implements --> RateLimiterPort
    Service -->|3. Lookup Config| RegistryPort
    Service -->|4. Atomic Check| RepoPort
    
    %% Data Flow
    RepoPort -.->|Returns| Result
    Service -.->|Returns| Result
    
    %% Driven Implementation
    RedisRepo -- implements --> RepoPort
    RedisRepo -->|5. EXEC| Lua
    Lua -->|6. R/W| RedisDB

    %% Support Wiring
    MemRegistry -- implements --> RegistryPort
    HeaderKey -- implements --> KeyPort
    
    %% Styling
    classDef core fill:#e3f2fd,stroke:#1565c0,stroke-width:2px;
    classDef port fill:#fff9c4,stroke:#fbc02d,stroke-width:2px;
    classDef adapter fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px;
    classDef db fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px;
    classDef user fill:#ffebee,stroke:#c62828,stroke-width:2px;

    class Service,Bucket,Config,Result,MemRegistry,HeaderKey core;
    class RateLimiterPort,RegistryPort,KeyPort,RepoPort,SourcePort port;
    class Aspect,WebAdapter,RedisRepo,Lua adapter;
    class RedisDB db;
    class User user;
```

### 2.2 Key Architectural Components

| Component | Layer | Responsibility |
| :--- | :--- | :--- |
| **`RateLimiter` (Port)** | Core (Inbound) | The primary API. Defines *what* the system does (allow/deny). |
| **`DefaultRateLimiter`** | Core (Service) | The "Brain". Orchestrates plan lookup, validation, and fail-open logic. |
| **`TokenBucket`** | Core (Model) | The "Math". Defines the refill algorithm. Pure, immutable, and side-effect free. |
| **`RateLimiterRepository`** | Core (Outbound) | The "Socket". Defines persistence needs. Does not know *how* data is stored. |
| **`RedisRateLimiterRepository`** | Infra (Adapter) | The "Plug". Implements the repository using Redis and Lua. |
| **`RateLimitAspect`** | Infra (Adapter) | The "Driver". Intercepts HTTP requests and drives the Core. |

### 2.3 Detailed Request Flow (Sequence)
```mermaid
sequenceDiagram
    participant U as User
    participant A as Aspect (@RateLimit)
    participant S as RateLimiterService
    participant P as PlanRegistry
    participant R as RedisAdapter
    participant DB as Redis (Cache)

    U->>A: GET /api/resource
    A->>S: allow(key, ["gold", "daily"], tokens=1)
    
    S->>P: getPlans(["gold", "daily"])
    P-->>S: List<RateLimitConfig>
    
    S->>R: tryAcquire(key, configs, tokens)
    
    alt Redis is Healthy
        R->>DB: EVALSHA (sha, key, args, epoch_time)
        DB-->>R: {allowed: 1, remaining: 5.0, wait: 0}
        R-->>S: RateLimitResult(allowed=true, ...)
        S-->>A: RateLimitResult
        A->>U: 200 OK
    else Redis Timeout
        R-->>S: throw RedisException
        S-->>S: Handle & Log (Fail-Open)
        S-->>A: RateLimitResult(allowed=true, reason="FAIL_OPEN")
        A->>U: 200 OK
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

1.  **Intercept**: Request hits a method annotated with `@RateLimit(key = "user", plan = "gold", tokens = 1)`.
2.  **Resolve**:
    *   `KeyResolver` extracts the user ID (e.g., `user:123`).
    *   `PlanRegistry` looks up "gold" config (Capacity: 10, Refill: 1/sec).
    *   **Weight**: The cost of the request (default 1) is identified.
3.  **Execute**:
    *   The `RateLimiterService` calls `repository.allow("user:123", rules, 1)`.
    *   Note: For **Chained Limits**, a list of rules is passed to ensure atomicity.
4.  **Redis Logic (Lua)**:
    *   **Get** current `tokens` and `last_refill` from Hash.
    *   **Time Source**: Get current Server Time (`redis.call('TIME')`) to prevent clock skew.
    *   **Calculate** refill: `delta = (now - last_refill) * rate`.
    *   **Update** tokens: `new_tokens = min(capacity, old_tokens + delta)`.
    *   **Check**: If `new_tokens >= tokens_to_consume`, decrement and return `true`. Else, return `false`.
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
| `ts` | Binary (8 bytes) | Last Refill Timestamp (Long, milliseconds/nanoseconds). |
| `v` | Integer | Schema Version (Currently `1`). |

*TTL is set to `Capacity / Rate` seconds (bucket lifetime).*

## 5. Class Design (Core)

```java
public interface RateLimiter {
    /** Orchestrates plan lookup and repository execution */
    RateLimitResult allow(String key, List<String> planNames, int tokens);
}

public record RateLimitConfig(
    String planName,
    long capacity,
    double tokensPerSecond
) {}

public record RateLimitResult(
    boolean allowed,
    double remainingTokens,
    long nanosToWait,
    String reason
) {}

public interface KeyResolver {
    String resolve(RequestSource source);
}
```

### 5.1 Provided Strategies (Core)
The library provides several out-of-the-box implementations in the Core module:
*   **`InMemoryPlanRegistry`**: Simple concurrent-map-based registry for testing or static rules.
*   **`HeaderKeyResolver`**: Extracts rate-limit identity from a specific HTTP header (e.g., `X-API-KEY`).
*   **`PrincipalKeyResolver`**: Uses the authenticated user's name as the rate-limit identity.

## 6. Resilience & Safety
*   **Fail Open**: If `EVALSHA` throws a `RedisConnectionException`, the library logs an error and returns `true` (Allowed).
*   **Self-Healing**: If `EVALSHA` throws `NOSCRIPT`, the library catches it, calls `SCRIPT LOAD`, and retries immediately.
