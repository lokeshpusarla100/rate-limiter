package com.lokesh.ratelimiter.core.port;

import com.lokesh.ratelimiter.core.model.RateLimitConfig;
import com.lokesh.ratelimiter.core.model.RateLimitResult;
import com.lokesh.ratelimiter.core.model.TokenBucket;

import java.util.List;
import java.util.Optional;

/**
 * Driven Port (Outbound): Repository interface for persisting and retrieving rate limit state.
 * 
 * Architectural Alignment:
 *   - ADR 001 (Hexagonal): Decouples domain logic from infrastructure (Redis, Memory).
 *   - ADR 004 (Atomic Operations): Implementations must guarantee that the 
 *       check-and-decrement cycle is atomic (e.g., via Lua scripts).
 *   - ADR 007 (Time Source): Implementations are responsible for sourcing 
 *       consistent time (e.g., {@code redis.call('TIME')}).
 */
public interface RateLimiterRepository {

    /**
     * Executes an atomic check-and-refill operation against multiple configurations.
     *
     * [Fix 5] Timeout Contract: Implementations MUST complete within 100ms 
     * or throw a {@link java.util.concurrent.TimeoutException}. This ensures that the 
     * rate limiter does not become a bottleneck or cause thread exhaustion in 
     * the calling application.
     *
     * @param key The unique identifier for the rate limit bucket.
     * @param configs The list of policies to evaluate concurrently.
     * @param tokensToConsume The weight of the current request.
     * @return A {@link RateLimitResult} representing the atomic outcome of all checks.
     */
    RateLimitResult tryAcquire(String key, List<RateLimitConfig> configs, int tokensToConsume);

    /**
     * Retrieves the current state of a specific bucket. 
     * Primarily for observability and testing.
     * 
     * @param key The unique identifier for the bucket.
     * @return An {@link Optional} containing the {@link TokenBucket} state.
     */
    Optional<TokenBucket> getState(String key);
}