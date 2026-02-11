package com.lokesh.ratelimiter.core.model;

/**
 * Public-facing result returned by a rate-limit check.
 *
 * <p>
 * This is the <strong>only</strong> type that external callers (controllers,
 * filters, API gateways) should interact with. It provides the final
 * allow/deny decision — potentially aggregated across multiple chained
 * limits — along with metadata useful for HTTP headers, logs, and metrics.
 *
 * <h3>RateLimitResult vs ConsumptionResult</h3>
 * <ul>
 * <li>{@link ConsumptionResult} — domain-internal output of a single
 * {@link TokenBucket #consume} call. Carries the mutated bucket state
 * so it can be persisted. Never leaves the domain layer.</li>
 * <li>{@code RateLimitResult} — this type. Hides all bucket internals
 * and adds caller-friendly fields ({@code reason}, fail-open support,
 * minimum remaining tokens across all limits).</li>
 * </ul>
 *
 * @param allowed         {@code true} if the request passed all evaluated
 *                        limits.
 * @param remainingTokens Minimum tokens left across all evaluated limits;
 *                        use this as the effective remaining budget.
 *                        A value of {@code -1} indicates unknown (fail-open).
 * @param waitMillis      If denied, how long (in milliseconds) to wait before
 *                        retrying.
 *                        Maps naturally to the {@code Retry-After} HTTP header.
 * @param reason          Short machine-friendly reason (e.g. {@code "OK"},
 *                        {@code "RATE_LIMITED"},
 *                        {@code "FAIL_OPEN: Redis timeout"}).
 */
public record RateLimitResult(
        boolean allowed,
        double remainingTokens,
        long waitMillis,
        String reason) {
    /**
     * Creates an allowed result.
     *
     * @param remainingTokens effective remaining tokens after the check.
     */
    public static RateLimitResult allow(double remainingTokens) {
        return new RateLimitResult(true, remainingTokens, 0, "OK");
    }

    /**
     * Creates a denied result.
     *
     * @param remainingTokens effective remaining tokens at denial time.
     * @param waitMillis      how long the caller should wait before retrying.
     * @param reason          denial reason for logs/observability.
     */
    public static RateLimitResult deny(double remainingTokens, long waitMillis, String reason) {
        return new RateLimitResult(false, remainingTokens, waitMillis, reason);
    }

    /**
     * Creates a fail-open result.
     *
     * <p>
     * Fail-open means the request is allowed because the limiter could not
     * evaluate safely due to an infrastructure error.
     *
     * @param reason source error or context string.
     */
    public static RateLimitResult failOpen(String reason) {
        // -1 means remaining tokens are unknown in fail-open mode.
        return new RateLimitResult(true, -1.0, 0, "FAIL_OPEN: " + reason);
    }
}
