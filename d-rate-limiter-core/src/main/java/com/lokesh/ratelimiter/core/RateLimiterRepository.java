package com.lokesh.ratelimiter.core;

import java.util.Optional;

/**
 * Driven Port (Outbound): Repository interface for persisting and retrieving rate limit state.
 *
 * <p>Architectural Alignment:
 * <ul>
 *   <li><b>ADR 001 (Hexagonal Architecture)</b>: Decouples core logic from infrastructure.
 *       This allows for multiple implementations (Redis, Hazelcast, In-Memory).</li>
 *   <li><b>ADR 004 (Distributed Strategy)</b>: Implementations must ensure atomicity
 *       during the check-and-decrement phase (e.g., using Lua in Redis).</li>
 * </ul>
 */
public interface RateLimiterRepository {

    /**
     * Executes an atomic check-and-refill operation.
     *
     * @param key The unique identifier for the rate limit bucket (e.g., "rate_limiter:gold:user_123").
     * @param config The policy to apply, including capacity and refill rate.
     * @param currentNanos The current reference time in nanoseconds to ensure consistent refill calculations.
     * @return {@code true} if a token was successfully deducted; {@code false} if the limit was exceeded.
     */
    boolean tryAcquire(String key, RateLimitConfig config, long currentNanos);

    /**
     * Retrieves the current state of a specific bucket.
     * Used primarily for observability, debugging, and testing.
     *
     * @param key The unique identifier for the bucket.
     * @return An {@link Optional} containing the {@link TokenBucket} state, or empty if it does not exist.
     */
    Optional<TokenBucket> getState(String key);
}
