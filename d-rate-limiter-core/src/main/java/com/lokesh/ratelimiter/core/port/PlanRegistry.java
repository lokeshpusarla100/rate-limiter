package com.lokesh.ratelimiter.core.port;

import com.lokesh.ratelimiter.core.model.RateLimitConfig;
import java.util.Optional;

/**
 * Port: Registry for managing and retrieving rate limit plans (Policies).
 *
 * <p>Architectural Alignment:
 * <ul>
 *   <li><b>ADR 005 (Hybrid Configuration)</b>: Decouples the "Binding" (Annotation)
 *       from the "Rule" (Config). This allows for centralized management and 
 *       dynamic updates of limits (e.g., via Redis or YAML).</li>
 * </ul>
 */
public interface PlanRegistry {

    /**
     * Retrieves a rate limit configuration by its plan name.
     *
     * @param planName The name of the plan (e.g., "gold", "silver").
     * @return An {@link Optional} containing the {@link RateLimitConfig}, or empty if not found.
     */
    Optional<RateLimitConfig> getPlan(String planName);
}