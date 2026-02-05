package com.lokesh.ratelimiter.core.support;

import com.lokesh.ratelimiter.core.model.RateLimitConfig;
import com.lokesh.ratelimiter.core.port.PlanRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple in-memory implementation of the {@link com.lokesh.ratelimiter.core.port.PlanRegistry}.
 * 
 * <p>Architectural Role: <b>Standard Support Implementation</b>.
 * This registry is suitable for local development, unit testing, or applications 
 * with static rate-limiting rules defined at startup.
 * 
 * <p>Implementation Details:
 * <ul>
 *   <li><b>Thread Safety</b>: Uses a {@link java.util.concurrent.ConcurrentHashMap} 
 *       to ensure thread-safe registration and lookup.</li>
 *   <li><b>Gap 9 Resolution</b>: Provided in the Core to reduce developer friction.</li>
 * </ul>
 */
public class InMemoryPlanRegistry implements PlanRegistry {

    private final Map<String, RateLimitConfig> plans = new ConcurrentHashMap<>();

    public void registerPlan(RateLimitConfig config) {
        plans.put(config.planName(), config);
    }

    @Override
    public Optional<RateLimitConfig> getPlan(String planName) {
        return Optional.ofNullable(plans.get(planName));
    }
}