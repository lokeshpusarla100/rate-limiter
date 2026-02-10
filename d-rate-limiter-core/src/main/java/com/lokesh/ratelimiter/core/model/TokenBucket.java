package com.lokesh.ratelimiter.core.model;

/**
 * Represents the state of a token bucket at a specific point in time.
 * 
 * <p>
 * Architectural Role: <b>State Entity</b>.
 * This record maps directly to the distributed state stored in Redis (ADR 004).
 * It is immutable; refill operations return a new instance rather than mutating
 * state.
 *
 * <p>
 * Implementation Details:
 * <ul>
 * <li><b>Epoch Consistency (ADR 007)</b>: Uses Epoch Milliseconds
 * (System.currentTimeMillis)
 * rather than nanoseconds to ensure coordinates are consistent across
 * distributed app instances.</li>
 * <li><b>Precision [Fix 9]</b>: Token counts are stored as doubles. This
 * provides sufficient
 * accuracy for rate limiting (error < 0.0001 tokens) but is not intended for
 * financial accounting.</li>
 * <li><b>Clock Skew [Fix 10]</b>: If {@code currentMillis <= lastRefillMillis},
 * the bucket state
 * is returned unchanged. This protects against backwards clock movement.</li>
 * </ul>
 * 
 * @param tokens           Current fractional token count.
 * @param lastRefillMillis Timestamp of the last successful update in epoch
 *                         milliseconds.
 */
public record TokenBucket(double tokens, long lastRefillMillis) {

    /**
     * Attempts to consume tokens after refilling the bucket based on the current
     * time.
     * 
     * <p>
     * <b>[Fix 6] Domain-Driven Logic</b>: This method encapsulates both the refill
     * and consumption logic, ensuring consistency between Java and Lua
     * implementations.
     *
     * @param now    The current time in epoch milliseconds.
     * @param cost   The number of tokens to consume.
     * @param config The policy to apply.
     * @return A {@link ConsumptionResult} containing the outcome and updated state.
     */
    public ConsumptionResult tryConsume(long now, int cost, RateLimitConfig config) {
        TokenBucket refilled = refill(now, config);

        if (refilled.tokens() >= cost) {
            TokenBucket updated = new TokenBucket(refilled.tokens() - cost, now);
            return ConsumptionResult.success(updated);
        }

        // Calculate wait time: (tokens_needed / refill_rate) * 1000ms
        double needed = cost - refilled.tokens();
        long waitMillis = (long) Math.ceil((needed / config.tokensPerSecond()) * 1000.0);

        return ConsumptionResult.denied(refilled, waitMillis);
    }

    /**
     * Refills the bucket based on elapsed time since the last refill.
     *
     * <p>
     * <b>Formula</b>:
     * {@code newTokens = min(capacity, currentTokens + elapsed_seconds × rate)}.
     *
     * <p>
     * <b>Clock-Skew Guard (ADR 007)</b>: If
     * {@code currentMillis <= lastRefillMillis}
     * (clock moved backwards or no time elapsed), the bucket is returned unchanged.
     * In production the time source is Redis ({@code redis.call('TIME')}), so this
     * guard is a safety net, not a common path.
     *
     * <p>
     * This method is <b>pure</b> — it returns a new {@code TokenBucket} and never
     * mutates {@code this}.
     *
     * @param currentMillis The current time in epoch milliseconds.
     * @param config        The policy whose capacity and rate define the refill
     *                      ceiling.
     * @return A new {@code TokenBucket} with updated token count and timestamp.
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