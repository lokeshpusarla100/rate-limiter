package com.lokesh.ratelimiter.core.model;

import java.util.Objects;

/**
 * Defines the Policy for a Rate Limit Plan.
 * 
 * <p>Architectural Role: <b>Policy Entity</b>.
 * This class holds the immutable rules (Capacity, Rate) that define a specific 
 * tier of service (e.g., "Gold", "Free"). It is segregated from the {@link TokenBucket},
 * which holds the volatile runtime state.
 *
 * <p>Implementation Details:
 * <ul>
 *   <li><b>Fail-Fast (ADR 002)</b>: Validates all inputs at construction time to 
 *       ensure no invalid configuration reaches the runtime engine.</li>
 *   <li><b>Identity (Epic 1.5)</b>: Includes {@code planName} to ensure unique 
 *       key namespacing in distributed storage.</li>
 * </ul>
 * 
 * @param planName Unique identifier for the plan (e.g., "gold"). Used for key generation.
 * @param capacity Maximum number of tokens the bucket can hold (Burst size).
 * @param tokensPerSecond The refill rate of the bucket.
 */
public record RateLimitConfig(String planName, long capacity, double tokensPerSecond) {

    public RateLimitConfig {
        Objects.requireNonNull(planName, "planName must not be null");
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
        if (tokensPerSecond <= 0) {
            throw new IllegalArgumentException("Tokens per second must be greater than zero");
        }
    }
}