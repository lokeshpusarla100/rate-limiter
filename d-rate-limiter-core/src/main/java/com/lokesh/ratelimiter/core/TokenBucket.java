package com.lokesh.ratelimiter.core;

/**
 * TDD Step 2: GREEN
 * The TokenBucket record represents the state of a rate limit bucket.
 * We use a Java Record to ensure immutability as per ADR 002.
 */
public record TokenBucket(long capacity, double refillRate, double currentTokens) {

    public TokenBucket(long capacity, double refillRate) {
        this(capacity, refillRate, (double) capacity);
    }

    /**
     * Refills the bucket based on the time elapsed.
     * @param nanosElapsed The time since the last refill in nanoseconds.
     * @return A new TokenBucket with updated token count.
     */
    public TokenBucket refill(long nanosElapsed) {
        if (nanosElapsed <= 0) {
            return this;
        }

        double tokensToAdd = (nanosElapsed / 1_000_000_000.0) * refillRate;
        double newTokens = Math.min(capacity, currentTokens + tokensToAdd);
        
        return new TokenBucket(capacity, refillRate, newTokens);
    }
}
