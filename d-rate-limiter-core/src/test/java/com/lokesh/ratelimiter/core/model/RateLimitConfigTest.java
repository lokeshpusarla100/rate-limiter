package com.lokesh.ratelimiter.core.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RateLimitConfigTest {

    @Test
    void shouldCreateValidConfig() {
        RateLimitConfig config = new RateLimitConfig("gold", 100, 50.5);
        assertEquals("gold", config.planName());
        assertEquals(100, config.capacity());
        assertEquals(50.5, config.tokensPerSecond());
    }

    @Test
    void shouldFailOnInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimitConfig("gold", 0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new RateLimitConfig("gold", -10, 1.0));
    }

    @Test
    void shouldFailOnInvalidRate() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimitConfig("gold", 10, 0));
        assertThrows(IllegalArgumentException.class, () -> new RateLimitConfig("gold", 10, -5.0));
    }
}