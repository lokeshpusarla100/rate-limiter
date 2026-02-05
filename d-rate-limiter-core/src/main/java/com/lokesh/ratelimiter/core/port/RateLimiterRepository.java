package com.lokesh.ratelimiter.core.port;

import com.lokesh.ratelimiter.core.model.RateLimitConfig;
import com.lokesh.ratelimiter.core.model.RateLimitResult;
import com.lokesh.ratelimiter.core.model.TokenBucket;

import java.util.List;
import java.util.Optional;

/**
 * Driven Port (Outbound): Repository interface for persisting and retrieving rate limit state.
 * 
 * <p>Architectural Alignment:
 * <ul>
 *   <li><b>ADR 001 (Hexagonal)</b>: Decouples domain logic from infrastructure (Redis, Memory).</li>
 *   <li><b>ADR 004 (Atomic Operations)</b>: Implementations must guarantee that the 
 *       check-and-decrement cycle is atomic (e.g., via Lua scripts).</li>
 *   <li><b>ADR 007 (Time Source)</b>: Implementations are responsible for sourcing 
 *       consistent time (e.g., {@code redis.call('TIME')}).</li>
 * </ul>
 */
public interface RateLimiterRepository {

    /**
     * Executes an atomic check-and-refill operation against multiple configurations.
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