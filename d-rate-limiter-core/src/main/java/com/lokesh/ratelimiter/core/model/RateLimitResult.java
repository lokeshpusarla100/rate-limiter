package com.lokesh.ratelimiter.core.model;

/**
 * The outcome of a rate limit check.
 * 
 * <p>Architectural Role: <b>Metadata Transfer Object</b>.
 * Instead of a simple boolean, this record carries the necessary context for 
 * driving adapters to generate helpful responses (e.g., HTTP 429 with Retry-After).
 *
 * @param allowed Whether the request passed all rate limit checks.
 * @param remainingTokens [Fix 4] The minimum tokens remaining across all evaluated buckets. 
 *                        Represents the most restrictive constraint.
 * @param waitMillis If denied, the duration the client should wait before a token becomes available.
 * @param reason A descriptive string for logging or debugging (e.g., "OK", "FAIL_OPEN").
 */
public record RateLimitResult(
    boolean allowed,
    double remainingTokens,
    long waitMillis,
    String reason
) {
    public static RateLimitResult allow(double remainingTokens) {
        return new RateLimitResult(true, remainingTokens, 0, "OK");
    }

    public static RateLimitResult deny(double remainingTokens, long waitMillis, String reason) {
        return new RateLimitResult(false, remainingTokens, waitMillis, reason);
    }

    public static RateLimitResult failOpen(String reason) {
        // -1 indicates unknown tokens due to failure
        return new RateLimitResult(true, -1.0, 0, "FAIL_OPEN: " + reason);
    }
}