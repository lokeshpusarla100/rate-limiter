package com.lokesh.ratelimiter.core.model;

/**
 * Internal result of a token consumption attempt within the domain model.
 * 
 * <p>Architectural Role: <b>Value Object</b>.
 * This record encapsulates the atomic outcome of refilling and consuming from a 
 * {@link TokenBucket}. It prevents the leak of domain logic into adapters.
 *
 * @param allowed True if the bucket had enough tokens to satisfy the request.
 * @param updatedBucket The new state of the bucket after consumption (if allowed) or refill.
 * @param waitMillis If denied, the number of milliseconds the client must wait for a token.
 */
public record ConsumptionResult(
    boolean allowed,
    TokenBucket updatedBucket,
    long waitMillis
) {
    /**
     * Creates a successful consumption result.
     * @param bucket The new state with tokens deducted.
     */
    public static ConsumptionResult success(TokenBucket bucket) {
        return new ConsumptionResult(true, bucket, 0);
    }

    /**
     * Creates a denied consumption result.
     * @param bucket The state after refill but without deduction.
     * @param waitMillis Time until next token availability.
     */
    public static ConsumptionResult denied(TokenBucket bucket, long waitMillis) {
        return new ConsumptionResult(false, bucket, waitMillis);
    }
}