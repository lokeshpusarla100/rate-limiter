package com.lokesh.ratelimiter.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD: Updated for Refactored Domain Model (State vs Policy).
 */
class TokenBucketTest {

    @Test
    void shouldRefillTokensBasedOnTimeAndConfig() {
        // GIVEN: A bucket with 5.0 tokens, last refilled at time 0
        TokenBucket bucket = new TokenBucket(5.0, 0L);
        RateLimitConfig config = new RateLimitConfig(10, 1.0); // 1 token/sec

        // WHEN: 1 second passes (1,000,000,000 nanos)
        long now = 1_000_000_000L;
        TokenBucket refilled = bucket.refill(now, config);

        // THEN: It should have 6.0 tokens
        assertEquals(6.0, refilled.tokens(), "Should have refilled 1 token in 1 second");
        assertEquals(now, refilled.lastRefillNanos(), "Should update last refill timestamp");
    }

    @Test
    void shouldNotExceedCapacityOnRefill() {
        // GIVEN: A bucket almost full (9.0), Capacity 10
        TokenBucket bucket = new TokenBucket(9.0, 0L);
        RateLimitConfig config = new RateLimitConfig(10, 10.0); // 10 tokens/sec

        // WHEN: 1 second passes (enough to overflow)
        long now = 1_000_000_000L;
        TokenBucket refilled = bucket.refill(now, config);

        // THEN: It should be capped at 10.0 tokens
        assertEquals(10.0, refilled.tokens(), "Should not exceed capacity");
    }

    @Test
    void shouldNotRefillIfTimeGoesBackwards() {
        // GIVEN: A bucket updated at time 100
        TokenBucket bucket = new TokenBucket(5.0, 100L);
        RateLimitConfig config = new RateLimitConfig(10, 1.0);

        // WHEN: Time is provided as 90 (older than last refill)
        TokenBucket refilled = bucket.refill(90L, config);

        // THEN: State should remain unchanged
        assertEquals(bucket, refilled);
    }
}
