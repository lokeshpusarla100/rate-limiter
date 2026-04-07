package com.lokesh.ratelimiter.core.port;

import com.lokesh.ratelimiter.core.model.RateLimitResult;
import java.util.List;

/**
 * Driving Port (Inbound): The primary service interface for performing rate limit checks.
 *
 * Architectural Alignment:
 *   - ADR 001 (Hexagonal): This is the primary inbound port for the Core Hexagon.
 *   - ADR 005 (Orchestration): Supports atomic checks against multiple plans
 *       to prevent partial token leaks.
 */
public interface RateLimiter {

    /**
     * Evaluates whether a request should be allowed against one or more plans.
     *
     * @param key The identity being limited (e.g., User ID, IP address).
     * @param planNames The names of the plans to evaluate (resolved via {@link PlanRegistry}).
     * @param tokensToConsume The number of tokens this specific request costs.
     * @return A {@link RateLimitResult} containing the decision and remaining token metadata.
     */
    RateLimitResult allow(String key, List<String> planNames, int tokensToConsume);
}