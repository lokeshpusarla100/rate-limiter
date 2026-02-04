package com.lokesh.ratelimiter.core;

/**
 * Represents the state of a token bucket at a specific point in time.
 * Designed to map directly to the Redis Hash structure defined in ADR 004.
 *
 * Fields:
 * - tokens: Current token count (Double precision).
 * - lastRefillNanos: Timestamp of the last refill (System.nanoTime).
 */
public record TokenBucket(double tokens, long lastRefillNanos) {

    /**
     * Calculates the new state of the bucket based on elapsed time and configuration.
     * Segregates State (this record) from Policy (RateLimitConfig).
     *
     * @param currentNanos The current server time in nanoseconds.
     * @param config The rate limit policy (capacity and rate).
     * @return A new TokenBucket instance with updated tokens and timestamp.
     */
    public TokenBucket refill(long currentNanos, RateLimitConfig config) {
        if (currentNanos <= lastRefillNanos) {
            return this;
        }

        long elapsedNanos = currentNanos - lastRefillNanos;
        double tokensToAdd = (elapsedNanos / 1_000_000_000.0) * config.tokensPerSecond();
        double newTokens = Math.min(config.capacity(), tokens + tokensToAdd);

        return new TokenBucket(newTokens, currentNanos);
    }
}
