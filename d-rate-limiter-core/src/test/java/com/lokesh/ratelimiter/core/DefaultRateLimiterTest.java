 package com.lokesh.ratelimiter.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * TDD: RED Phase for DefaultRateLimiter.
 *
 * This test defines the behavior of the Driving Port implementation,
 * specifically ensuring compliance with:
 * - ADR 002: Fail-Open Resilience.
 * - ADR 001: Separation of concerns.
 */
@ExtendWith(MockitoExtension.class)
class DefaultRateLimiterTest {

    @Mock
    private RateLimiterRepository repository;

    @InjectMocks
    private DefaultRateLimiter rateLimiter;

    private final RateLimitConfig config = new RateLimitConfig(10, 1.0);
    private final String key = "test-user";

    @Test
    @DisplayName("Should return true when repository allows the request")
    void shouldAllowWhenRepositorySucceeds() {
        // GIVEN: Repository returns true
        when(repository.tryAcquire(eq(key), eq(config), anyLong())).thenReturn(true);

        // WHEN: We check allowance
        boolean allowed = rateLimiter.allow(key, config);

        // THEN: Result should be true
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("Should return false when repository denies the request")
    void shouldDenyWhenRepositoryExceeded() {
        // GIVEN: Repository returns false (limit reached)
        when(repository.tryAcquire(eq(key), eq(config), anyLong())).thenReturn(false);

        // WHEN: We check allowance
        boolean allowed = rateLimiter.allow(key, config);

        // THEN: Result should be false
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("Should FAIL OPEN (return true) when repository throws an exception")
    void shouldFailOpenOnRepositoryError() {
        // GIVEN: Repository crashes (e.g., Redis Connection Timeout)
        when(repository.tryAcquire(any(), any(), anyLong()))
                .thenThrow(new RuntimeException("Redis is down"));

        // WHEN: We check allowance
        boolean allowed = rateLimiter.allow(key, config);

        // THEN: Result must be true (ADR 002 - Never block user traffic due to infra failure)
        assertThat(allowed).isTrue();
    }
}
