package com.lokesh.ratelimiter.core.model;

/**
 * Internal domain result of a consumption attempt.
 */
public record ConsumptionResult(
    boolean allowed,
    TokenBucket updatedBucket,
    long waitMillis
) {
    public static ConsumptionResult success(TokenBucket bucket) {
        return new ConsumptionResult(true, bucket, 0);
    }

    public static ConsumptionResult denied(TokenBucket bucket, long waitMillis) {
        return new ConsumptionResult(false, bucket, waitMillis);
    }
}
