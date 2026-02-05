package com.lokesh.ratelimiter.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketPrecisionTest {

    @Test
    @DisplayName("[Fix 9] Verify floating point precision drift over 1 million refill operations")
    void verifyPrecisionDrift() {
        // GIVEN: 1 token/sec, Capacity 1M
        RateLimitConfig config = new RateLimitConfig("precision", 1_000_000, 1.0);
        TokenBucket bucket = new TokenBucket(0.0, 0L);
        
        // WHEN: Refilling 1ms at a time, 1 million times (Total 1000 seconds)
        for (int i = 1; i <= 1_000_000; i++) {
            bucket = bucket.refill(i, config); // 1ms elapsed each time
        }
        
        // THEN: Total tokens should be exactly 1000.0 (1ms * 1M = 1000s * 1 token/s)
        // We expect some precision error, but it should be very small (< 0.01)
        double expected = 1000.0;
        double actual = bucket.tokens();
        
        assertThat(actual).isCloseTo(expected, org.assertj.core.data.Offset.offset(0.01));
        System.out.println("Final token count after 1M refills: " + actual + " (Error: " + Math.abs(expected - actual) + ")");
    }
}
