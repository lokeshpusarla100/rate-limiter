package com.lokesh.ratelimiter.core;


/**
 * Defines the Policy for a Rate Limit Plan.
 * Segregates Configuration (Capacity/Rate) from State (TokenBucket).
 *
 * Implements "Fail Fast" (ADR 002) - validates inputs at construction.
 */
public record RateLimitConfig(long capacity, double tokensPerSecond) {

    public RateLimitConfig {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
        if (tokensPerSecond <= 0) {
            throw new IllegalArgumentException("Tokens per second must be greater than zero");
        }
    }
}
