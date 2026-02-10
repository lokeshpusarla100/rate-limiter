package com.lokesh.ratelimiter.core.service;

import com.lokesh.ratelimiter.core.model.RateLimitConfig;
import com.lokesh.ratelimiter.core.model.RateLimitResult;
import com.lokesh.ratelimiter.core.port.PlanRegistry;
import com.lokesh.ratelimiter.core.port.RateLimiterRepository;
import com.lokesh.ratelimiter.core.port.RateLimitEventListener;
import com.lokesh.ratelimiter.core.support.MissingPlanPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TDD: Updated for Hardened DefaultRateLimiter.
 */
@ExtendWith(MockitoExtension.class)
class DefaultRateLimiterTest {

    @Mock
    private RateLimiterRepository repository;

    @Mock
    private PlanRegistry planRegistry;

    @Mock
    private RateLimitEventListener listener;

    @InjectMocks
    private DefaultRateLimiter rateLimiter;

    private final RateLimitConfig config = new RateLimitConfig("gold", 10, 1.0);
    private final String key = "test-user";

    @Test
    @DisplayName("Should FAIL_FAST when plan is missing by default")
    void shouldFailFastOnMissingPlan() {
        // GIVEN: Default policy (FAIL_FAST)
        rateLimiter = new DefaultRateLimiter(repository, planRegistry, List.of(listener), MissingPlanPolicy.FAIL_FAST);
        when(planRegistry.getPlan("missing")).thenReturn(Optional.empty());

        // WHEN/THEN
        assertThatThrownBy(() -> rateLimiter.allow(key, List.of("missing"), 1))
                .isInstanceOf(IllegalArgumentException.class);

        verify(listener).onPlanMissing("missing");
    }

    @Test
    @DisplayName("Should notify onAllow when repository succeeds")
    void shouldNotifyOnAllow() {
        rateLimiter = new DefaultRateLimiter(repository, planRegistry, List.of(listener),
                MissingPlanPolicy.SKIP_WITH_WARN);
        RateLimitResult expectedResult = RateLimitResult.allow(9.0);

        when(planRegistry.getPlan("gold")).thenReturn(Optional.of(config));
        when(repository.tryAcquire(anyString(), anyList(), anyInt())).thenReturn(expectedResult);

        // WHEN
        rateLimiter.allow(key, List.of("gold"), 1);

        // THEN
        verify(listener).onAllow(eq(key), eq(List.of("gold")), eq(expectedResult));
    }

    @Test
    @DisplayName("Should FAIL OPEN and notify on repository error")
    void shouldFailOpenAndNotify() {
        rateLimiter = new DefaultRateLimiter(repository, planRegistry, List.of(listener),
                MissingPlanPolicy.SKIP_WITH_WARN);
        when(planRegistry.getPlan("gold")).thenReturn(Optional.of(config));
        when(repository.tryAcquire(any(), any(), anyInt())).thenThrow(new RuntimeException("Redis down"));

        // WHEN
        RateLimitResult result = rateLimiter.allow(key, List.of("gold"), 1);

        // THEN
        assertThat(result.allowed()).isTrue();
        verify(listener).onFailOpen(eq(key), contains("Redis down"));
    }
}