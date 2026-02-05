package com.lokesh.ratelimiter.core.support;

import com.lokesh.ratelimiter.core.model.RateLimitConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryPlanRegistryTest {

    private final InMemoryPlanRegistry registry = new InMemoryPlanRegistry();

    @Test
    @DisplayName("Should register and retrieve a plan successfully")
    void shouldRegisterAndRetrievePlan() {
        // GIVEN
        RateLimitConfig gold = new RateLimitConfig("gold", 100, 10.0);

        // WHEN
        registry.registerPlan(gold);
        Optional<RateLimitConfig> retrieved = registry.getPlan("gold");

        // THEN
        assertThat(retrieved).isPresent().contains(gold);
    }

    @Test
    @DisplayName("Should return empty Optional when plan name is not found")
    void shouldReturnEmptyWhenPlanNotFound() {
        // WHEN
        Optional<RateLimitConfig> retrieved = registry.getPlan("non-existent");

        // THEN
        assertThat(retrieved).isEmpty();
    }

    @Test
    @DisplayName("Should overwrite plan if registered with the same name")
    void shouldOverwritePlan() {
        // GIVEN
        RateLimitConfig v1 = new RateLimitConfig("gold", 100, 10.0);
        RateLimitConfig v2 = new RateLimitConfig("gold", 200, 20.0);

        // WHEN
        registry.registerPlan(v1);
        registry.registerPlan(v2);
        Optional<RateLimitConfig> retrieved = registry.getPlan("gold");

        // THEN
        assertThat(retrieved).isPresent().contains(v2);
    }
}
