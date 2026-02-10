package com.lokesh.ratelimiter.core.model;

/**
 * Internal result of a <strong>single</strong> token-bucket consume attempt.
 *
 * <p>
 * This record is used exclusively within the domain layer by
 * {@link TokenBucket#consume} to return both the allow/deny decision and
 * the <em>mutated bucket state</em> so the caller can persist it.
 * It is never exposed to external callers.
 *
 * <h3>ConsumptionResult vs RateLimitResult</h3>
 * <ul>
 * <li>{@code ConsumptionResult} — raw output of one bucket's math
 * (carries the updated {@link TokenBucket}).</li>
 * <li>{@link RateLimitResult} — public-facing response returned by
 * {@code DefaultRateLimiter.tryConsume()}, aggregated across all
 * evaluated limits, with a human/machine-friendly reason and
 * fail-open support. It never exposes internal bucket state.</li>
 * </ul>
 *
 * @param allowed       {@code true} if this bucket had enough tokens for the
 *                      request.
 * @param updatedBucket Bucket state after refill and (if allowed) token
 *                      deduction.
 * @param waitMillis    If denied, milliseconds the caller must wait before this
 *                      bucket would have enough tokens to grant a retry.
 */
public record ConsumptionResult(
        boolean allowed,
        TokenBucket updatedBucket,
        long waitMillis) {
    /**
     * Creates an allowed consumption result.
     *
     * @param bucket updated state with tokens deducted.
     */
    public static ConsumptionResult success(TokenBucket bucket) {
        return new ConsumptionResult(true, bucket, 0);
    }

    /**
     * Creates a denied consumption result.
     *
     * @param bucket     state after refill, without deduction.
     * @param waitMillis wait time before enough tokens are available.
     */
    public static ConsumptionResult denied(TokenBucket bucket, long waitMillis) {
        return new ConsumptionResult(false, bucket, waitMillis);
    }
}
