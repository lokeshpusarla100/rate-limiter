package com.lokesh.ratelimiter.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Step 1: RED
 * This test describes how the TokenBucket should be initialized.
 * It will fail to compile because TokenBucket does not exist yet.
 */
class TokenBucketTest {

    @Test
    void shouldInitializeWithFullCapacity() {
        long capacity = 10;
        double refillRate = 1.0;

        TokenBucket bucket = new TokenBucket(capacity, refillRate);

        assertEquals(capacity, bucket.capacity());
        assertEquals(capacity, (long) bucket.currentTokens());
    }

    @Test
    void shouldRefillTokensOverTime() {
        // GIVEN: A bucket with 5/10 tokens, rate 1 token/sec
        TokenBucket bucket = new TokenBucket(10, 1.0, 5.0);
        
        // WHEN: 1 second passes (1,000,000,000 nanos)
        TokenBucket refilled = bucket.refill(1_000_000_000L);

        // THEN: It should have 6.0 tokens
        assertEquals(6.0, refilled.currentTokens(), "Should have refilled 1 token in 1 second");
    }

    @Test
    void shouldNotExceedCapacityOnRefill() {
        // GIVEN: A bucket with 9/10 tokens, rate 10 tokens/sec
        TokenBucket bucket = new TokenBucket(10, 10.0, 9.0);

        // WHEN: 1 second passes (10 tokens would be added)
        TokenBucket refilled = bucket.refill(1_000_000_000L);

        // THEN: It should be capped at 10.0 tokens
        assertEquals(10.0, refilled.currentTokens(), "Should not exceed capacity");
    }
}
