# Project Reorientation Guide

This file is a practical map of the repository as it exists now.
It is meant to answer four questions quickly:

1. What is this project?
2. What is actually implemented?
3. What patterns does it use?
4. How should I describe it honestly in an interview?

## 1. What This Project Is

`d-rate-limiter` is a multi-module Java library for distributed rate limiting.
The design goal is to keep the rate-limiting rules inside a small domain core and
push infrastructure details like Redis, HTTP, and Spring into outer modules.

The main algorithm is a token bucket:

- each identity has a bucket with a maximum capacity
- tokens refill over time at a configured rate
- each request consumes one or more tokens
- if enough tokens remain, the request is allowed
- otherwise the request is denied and a wait time is returned

## 2. Repository Layout

### `d-rate-limiter-core`

This is the real heart of the project and the most complete module.

It contains:

- domain models such as `TokenBucket`, `RateLimitConfig`, and `RateLimitResult`
- ports such as `RateLimiter`, `PlanRegistry`, `RateLimiterRepository`, and `KeyResolver`
- the main orchestration service `DefaultRateLimiter`
- support utilities like `InMemoryPlanRegistry`, `HeaderKeyResolver`, and `RateLimitKey`

This module is intentionally light on framework dependencies and is the best
representation of the project's design quality.

### `d-rate-limiter-redis`

This module is partially implemented.

It currently contains:

- a Lua script for atomic token acquisition
- a Lua script loader utility
- tests around script loading and script execution

It does not currently contain a full `RedisRateLimiterRepository`
implementation that plugs into the core `RateLimiterRepository` port.

### `d-rate-limiter-spring-boot-starter`

This module is scaffolding right now.

It has a `pom.xml` and declared dependencies, but no production source files.
The intended role is auto-configuration, AOP interception, and request adapter
wiring for Spring applications.

### `d-rate-limiter-samples`

This module is also scaffolding right now.

It exists at the build level, but there is no sample application source yet.

## 3. Core Runtime Flow

The implemented core flow is:

1. A caller invokes `RateLimiter.allow(key, planNames, tokensToConsume)`.
2. `DefaultRateLimiter` resolves plan names using `PlanRegistry`.
3. Missing plans are handled according to `MissingPlanPolicy`.
4. If no valid plans remain, the request is allowed with `remainingTokens = -1`.
5. If plans are resolved, `DefaultRateLimiter` delegates the actual atomic
   token acquisition to `RateLimiterRepository`.
6. The returned `RateLimitResult` determines whether listeners get `onAllow`
   or `onDeny`.
7. If the repository throws, the service fails open and returns an allowed
   result with a `FAIL_OPEN` reason.

The important design point is that the core does not know whether the
repository is backed by Redis, memory, or something else.

## 4. Main Patterns In Use

### Hexagonal Architecture

The project is organized around ports and adapters:

- inbound ports define what the system does
- outbound ports define what the system needs from infrastructure
- adapters implement those ports outside the core

Example:

- `RateLimiter` is the inbound port
- `DefaultRateLimiter` is the core service implementation
- `RateLimiterRepository` is the outbound port
- a future Redis adapter would implement `RateLimiterRepository`

Why this matters:

- the token bucket logic stays testable
- framework code does not leak into the core
- Redis can be replaced without rewriting domain logic

### Domain-Driven Separation

The code distinguishes between:

- policy: `RateLimitConfig`
- state: `TokenBucket`
- orchestration: `DefaultRateLimiter`
- integration contracts: the `port` package

That separation is one of the better qualities of the project.

### Fail-Open Resilience

The service intentionally allows traffic when the infrastructure layer fails.
That is a business choice: service availability is treated as more important
than strict enforcement during backend outages.

This is implemented in `DefaultRateLimiter` by catching repository exceptions
and returning `RateLimitResult.failOpen(...)`.

### Strategy Pattern

`KeyResolver` is a strategy interface.
Different resolvers can decide how to identify the caller:

- by header
- by principal
- later by IP, API key, tenant, etc.

### Builder Utility

`RateLimitKey` uses a builder to create consistent Redis-style bucket keys such
as `ratelimiter:tenant:user:plan`.

## 5. The Most Important Classes

### `TokenBucket`

This is the rate-limiting math.

Responsibilities:

- refill tokens based on elapsed time
- cap tokens at bucket capacity
- consume tokens if enough are available
- compute wait time if tokens are insufficient

This is the clearest place to discuss the algorithm itself.

### `DefaultRateLimiter`

This is the orchestration layer.

Responsibilities:

- plan resolution
- missing-plan handling
- repository delegation
- event emission
- fail-open behavior

This is the clearest place to discuss business behavior and architecture.

### `RateLimiterRepository`

This is the abstraction that makes the design distributed-ready.

Its contract says the acquire operation should be atomic across the evaluated
plans. The Redis module is clearly intended to satisfy that requirement using Lua.

### `acquire_token.lua`

This script shows how distributed atomicity was intended to work:

- fetch bucket state and config from Redis
- use Redis server time
- refill and consume inside one script
- persist state only on success
- return allowed/remaining/wait data

Important note:
the current Lua script handles one bucket/config pair, while the docs talk
about atomic chained multi-plan limits. So the design intention is ahead of the
current adapter implementation.

## 6. What Is Actually Finished

Reasonably complete:

- core domain model
- core service orchestration
- plan registry support
- key resolver support
- unit tests for most core logic
- Lua script loader
- initial Redis Lua script

Partially complete:

- Redis integration at the script and test level

Not yet implemented in production code:

- Redis repository adapter wiring into the core port
- Spring Boot auto-configuration
- AOP or annotation-based request interception
- sample application
- observability adapters such as metrics exporters

## 7. Interview-Safe Project Summary

If you need a short and defensible description, use something close to this:

"I built a Java 21 multi-module rate-limiting library around a token bucket
algorithm. The core module follows a ports-and-adapters design, so the rate
limit math and orchestration stay independent from Redis and Spring. I also
implemented the fail-open behavior, plan resolution, key resolution strategies,
and initial Redis Lua scripting for atomic bucket updates."

That is stronger and safer than claiming the full Spring starter and distributed
adapter are production complete.

## 8. Questions You Should Be Ready To Answer

### Why hexagonal architecture here?

Because rate-limiting rules are stable business logic, while delivery
mechanisms and storage backends are replaceable integration details.

### Why token bucket instead of fixed window?

Token bucket supports burst handling more naturally and gives smoother traffic
control than a naive fixed-window limiter.

### Why use Redis Lua?

Because distributed rate limiting needs atomic read-modify-write behavior.
Lua lets Redis execute the whole decision as one server-side operation.

### Why fail open?

Because in many production systems it is better to temporarily allow excess
traffic than to take the entire service down when Redis is unavailable.

### What part is the strongest in this repo?

The core module. It is the most complete and best aligned with the intended
architecture.

### What part is weakest or unfinished?

The outer adapters. The starter and samples are scaffolding, and the Redis
module is not yet fully wired as a repository implementation.

## 9. If You Need To Rebuild Context Fast

Read files in this order:

1. `README.md`
2. `d-rate-limiter-core/src/main/java/.../port/RateLimiter.java`
3. `d-rate-limiter-core/src/main/java/.../service/DefaultRateLimiter.java`
4. `d-rate-limiter-core/src/main/java/.../model/TokenBucket.java`
5. `d-rate-limiter-core/src/main/java/.../model/RateLimitResult.java`
6. `d-rate-limiter-core/src/main/java/.../port/RateLimiterRepository.java`
7. `d-rate-limiter-redis/src/main/resources/lua/acquire_token.lua`

If those make sense, the rest of the repository becomes much easier to follow.

## 10. Current Reality Check

This repo is best described as:

- a well-structured core domain for a distributed rate limiter
- an initial Redis scripting layer
- an incomplete outer integration layer

That is still legitimate work. Just describe it as an architecture-first library
with a complete core and partial infrastructure adapters, not as a fully
finished production starter.
