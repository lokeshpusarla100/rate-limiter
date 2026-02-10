package com.lokesh.ratelimiter.core.support;

/**
 * Standardized utility for generating Redis keys for rate limiting.
 * 
 * <p>
 * Architectural Role: <b>Standardization Support</b>. [Fix 8]
 * Follows the format: {@code ratelimiter:{tenant}:{user}:{plan}}
 * 
 * <p>
 * Standardization prevents key collisions in distributed environments and
 * simplifies monitoring and debugging.
 */
public class RateLimitKey {

    private final String tenant;
    private final String user;
    private final String plan;

    /** Private — use {@link #builder()} to construct instances. */
    private RateLimitKey(String tenant, String user, String plan) {
        this.tenant = tenant;
        this.user = user;
        this.plan = plan;
    }

    /**
     * @return A new builder for constructing a standard key.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Generates the final Redis key string.
     *
     * <p>
     * <b>Format</b>: {@code ratelimiter:{tenant}:{user}:{plan}}.<br>
     * Missing segments default to {@code "default"}, {@code "anonymous"},
     * and {@code "global"} respectively.
     *
     * @return a fully-qualified, colon-delimited key safe for use as a Redis key.
     */
    @Override
    public String toString() {
        return String.format("ratelimiter:%s:%s:%s",
                tenant != null ? tenant : "default",
                user != null ? user : "anonymous",
                plan != null ? plan : "global");
    }

    /**
     * Fluent builder for {@link RateLimitKey}.
     */
    public static class Builder {
        private String tenant;
        private String user;
        private String plan;

        /**
         * Sets the tenant segment.
         *
         * @param tenant the tenant ID (e.g., {@code "acme-corp"}). Defaults to
         *               {@code "default"} if unset.
         * @return this builder for chaining.
         */
        public Builder withTenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        /**
         * Sets the user segment.
         *
         * @param user the user ID or client identifier (e.g., {@code "user_123"}).
         *             Defaults to {@code "anonymous"} if unset.
         * @return this builder for chaining.
         */
        public Builder withUser(String user) {
            this.user = user;
            return this;
        }

        /**
         * Sets the plan segment.
         *
         * @param plan the rate-limit plan name (e.g., {@code "gold"}). Defaults to
         *             {@code "global"} if unset.
         * @return this builder for chaining.
         */
        public Builder withPlan(String plan) {
            this.plan = plan;
            return this;
        }

        /**
         * Builds the final Redis key string.
         *
         * @return a fully-qualified key in the format
         *         {@code ratelimiter:{tenant}:{user}:{plan}}.
         */
        public String build() {
            return new RateLimitKey(tenant, user, plan).toString();
        }
    }
}