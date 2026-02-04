package com.lokesh.ratelimiter.core;

/**
 * Driving Port (Inbound): The primary service interface for performing rate limit checks.
 *
 * <p>Architectural Alignment:
 * <ul>
 *   <li><b>ADR 002 (Defensive Programming)</b>: Defines the "Fail-Safe" contract.
 *       If the underlying implementation (e.g., Redis) fails, the system should default
 *       to allowing the request ("Fail Open").</li>
 *   <li><b>ADR 005 (Multi-Tenancy)</b>: Supports applying different configurations
 *       to different keys dynamically.</li>
 * </ul>
 */
public interface RateLimiter {

    /**
     * Evaluates whether a request should be allowed based on the provided configuration.
     *
     * @param key The identity being limited (e.g., IP address, User ID).
     * @param config The rate limit rules (Capacity, Tokens Per Second).
     * @return {@code true} if the request is allowed (either within limits or system failure);
     *         {@code false} if the rate limit has been strictly exceeded.
     */
    boolean allow(String key, RateLimitConfig config);
}
