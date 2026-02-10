package com.lokesh.ratelimiter.core.exception;

/**
 * Exception thrown when a rate limit has been exceeded.
 *
 * <p>
 * Following the Design Document (Section 3), this exception is used by the
 * Driving Adapter (Aspect) to signal that a request should be blocked
 * (typically resulting in a 429 Too Many Requests HTTP response).
 */
public class RateLimitExceededException extends RuntimeException {

    private final String key;
    private final String planName;

    /**
     * Creates a new exception indicating rate-limit exhaustion.
     *
     * @param key      the identity that was rate-limited (e.g.,
     *                 {@code "user:123"}).
     * @param planName the plan whose tokens were exhausted (e.g., {@code "gold"}).
     */
    public RateLimitExceededException(String key, String planName) {
        super(String.format("Rate limit exceeded for key [%s] using plan [%s]", key, planName));
        this.key = key;
        this.planName = planName;
    }

    /** @return the identity that was rate-limited. */
    public String getKey() {
        return key;
    }

    /** @return the plan whose tokens were exhausted. */
    public String getPlanName() {
        return planName;
    }
}