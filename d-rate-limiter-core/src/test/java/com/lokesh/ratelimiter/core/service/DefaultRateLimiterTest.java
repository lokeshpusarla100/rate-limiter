package com.lokesh.ratelimiter.core.service;

import com.lokesh.ratelimiter.core.model.RateLimitConfig;
import com.lokesh.ratelimiter.core.model.RateLimitResult;
import com.lokesh.ratelimiter.core.port.PlanRegistry;
import com.lokesh.ratelimiter.core.port.RateLimiterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * TDD: Updated for Refactored DefaultRateLimiter.
 */
@ExtendWith(MockitoExtension.class)
class DefaultRateLimiterTest {

    @Mock
    private RateLimiterRepository repository;

    @Mock
    private PlanRegistry planRegistry;

    @InjectMocks
    private DefaultRateLimiter rateLimiter;

    private final RateLimitConfig config = new RateLimitConfig("gold", 10, 1.0);
    private final String key = "test-user";

    @Test
    @DisplayName("Should return allowed result when repository allows the request")
    void shouldAllowWhenRepositorySucceeds() {
        // GIVEN: Plan exists and repository returns success
        when(planRegistry.getPlan("gold")).thenReturn(Optional.of(config));
        when(repository.tryAcquire(eq(key), eq(List.of(config)), eq(1)))
                .thenReturn(RateLimitResult.allow(9.0));

        // WHEN: We check allowance
        RateLimitResult result = rateLimiter.allow(key, List.of("gold"), 1);

        // THEN: Result should be allowed
        assertThat(result.allowed()).isTrue();
        assertThat(result.remainingTokens()).isEqualTo(9.0);
    }

    @Test
    @DisplayName("Should return denied result when repository denies the request")
    void shouldDenyWhenRepositoryExceeded() {
        // GIVEN: Plan exists and repository returns denial
        when(planRegistry.getPlan("gold")).thenReturn(Optional.of(config));
        when(repository.tryAcquire(eq(key), eq(List.of(config)), eq(1)))
                .thenReturn(RateLimitResult.deny(0.0, 1000L, "Exceeded"));

        // WHEN: We check allowance
        RateLimitResult result = rateLimiter.allow(key, List.of("gold"), 1);

        // THEN: Result should be denied
        assertThat(result.allowed()).isFalse();
        assertThat(result.waitMillis()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("Should FAIL OPEN when repository throws an exception")
    void shouldFailOpenOnRepositoryError() {
        // GIVEN: Plan exists but repository crashes
        when(planRegistry.getPlan("gold")).thenReturn(Optional.of(config));
        when(repository.tryAcquire(any(), any(), anyInt()))
                .thenThrow(new RuntimeException("Redis is down"));

        // WHEN: We check allowance
        RateLimitResult result = rateLimiter.allow(key, List.of("gold"), 1);

        // THEN: Result must be allowed (Fail-Open)
        assertThat(result.allowed()).isTrue();
        assertThat(result.reason()).contains("FAIL_OPEN");
    }
}