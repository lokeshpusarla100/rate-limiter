package com.lokesh.ratelimiter.core.support;

import com.lokesh.ratelimiter.core.model.RateLimitConfig;
import com.lokesh.ratelimiter.core.port.PlanRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple in-memory implementation of the
 * {@link com.lokesh.ratelimiter.core.port.PlanRegistry}.
 * 
 *
 * Standard Support Implementation.
 * This registry is suitable for local development, unit testing, or
 * applications
 * with static rate-limiting rules defined at startup.
 * 
 *
 * Implementation Details:
 * - Thread Safety: Uses a
 * {@link java.util.concurrent.ConcurrentHashMap}
 * to ensure thread-safe registration and lookup.
 * - Gap 9 Resolution: Provided in the Core to reduce developer
 * friction.
 */
public class InMemoryPlanRegistry implements PlanRegistry {

    private final Map<String, RateLimitConfig> plans = new ConcurrentHashMap<>();

    /**
     * Registers (or overwrites) a plan configuration.
     *
     * @param config the plan to register; its {@link RateLimitConfig#planName()} is
     *               used as the key.
     */
    public void registerPlan(RateLimitConfig config) {
        plans.put(config.planName(), config);
    }

    @Override
    public Optional<RateLimitConfig> getPlan(String planName) {
        return Optional.ofNullable(plans.get(planName));
    }
}