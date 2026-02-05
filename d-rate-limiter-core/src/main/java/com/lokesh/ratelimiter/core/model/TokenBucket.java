package com.lokesh.ratelimiter.core.model;

/**
 * Represents the state of a token bucket at a specific point in time.
 * 
 * <p>Architectural Role: <b>State Entity</b>.
 * This record maps directly to the distributed state stored in Redis (ADR 004).
 * It is immutable; refill operations return a new instance rather than mutating state.
 *
 * <p>Implementation Details:
 * <ul>
 *   <li><b>Epoch Consistency (ADR 007)</b>: Uses Epoch Milliseconds (System.currentTimeMillis)
 *       rather than nanoseconds to ensure coordinates are consistent across distributed app instances.</li>
 *   <li><b>Precision</b>: Token counts are stored as doubles to maintain accuracy during 
 *       partial refills over small time deltas.</li>
 * </ul>
 * 
 * @param tokens Current fractional token count.
 * @param lastRefillMillis Timestamp of the last successful update in epoch milliseconds.
 */
public record TokenBucket(double tokens, long lastRefillMillis) {

    /**
     * Calculates the new state of the bucket based on elapsed time and a specific policy.
     * 
     * <p>This method implements the core Token Bucket algorithm. It is side-effect free
     * and relies on the caller to provide the current time, facilitating testing 
     * and distributed time synchronization.
     *
     * @param currentMillis The reference time to calculate refill from.
     * @param config The policy (capacity/rate) to apply to this state.
     * @return A new {@link TokenBucket} representing the state after the refill.
     */
    public TokenBucket refill(long currentMillis, RateLimitConfig config) {
        if (currentMillis <= lastRefillMillis) {
            return this;
        }

        long elapsedMillis = currentMillis - lastRefillMillis;
        double tokensToAdd = (elapsedMillis / 1000.0) * config.tokensPerSecond();
        double newTokens = Math.min(config.capacity(), tokens + tokensToAdd);

        return new TokenBucket(newTokens, currentMillis);
    }
}