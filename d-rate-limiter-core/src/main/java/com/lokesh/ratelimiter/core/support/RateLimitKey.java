package com.lokesh.ratelimiter.core.support;

import java.util.Objects;

/**
 * Standardized utility for generating Redis keys for rate limiting.
 * 
 * <p>Architectural Role: <b>Standardization Support</b>. [Fix 8]
 * Follows the format: {@code ratelimiter:{tenant}:{user}:{plan}}
 * 
 * <p>Standardization prevents key collisions in distributed environments and 
 * simplifies monitoring and debugging.
 */
public class RateLimitKey {
    
    private final String tenant;
    private final String user;
    private final String plan;

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
     * Generates the final Redis key string using defaults for missing fields.
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
         * @param tenant The tenant ID (e.g., "acme-corp"). Defaults to "default".
         */
        public Builder withTenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        /**
         * @param user The user ID or client identifier. Defaults to "anonymous".
         */
        public Builder withUser(String user) {
            this.user = user;
            return this;
        }

        /**
         * @param plan The name of the limit plan. Defaults to "global".
         */
        public Builder withPlan(String plan) {
            this.plan = plan;
            return this;
        }

        /**
         * @return The formatted key string.
         */
        public String build() {
            return new RateLimitKey(tenant, user, plan).toString();
        }
    }
}