package com.lokesh.ratelimiter.core.support;

import java.util.Objects;

/**
 * [Fix 8] Builder for standardized rate limit keys.
 * Format: "ratelimiter:{tenant}:{user}:{plan}"
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

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("ratelimiter:%s:%s:%s", 
                tenant != null ? tenant : "default",
                user != null ? user : "anonymous",
                plan != null ? plan : "global");
    }

    public static class Builder {
        private String tenant;
        private String user;
        private String plan;

        public Builder withTenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        public Builder withUser(String user) {
            this.user = user;
            return this;
        }

        public Builder withPlan(String plan) {
            this.plan = plan;
            return this;
        }

        public String build() {
            return new RateLimitKey(tenant, user, plan).toString();
        }
    }
}
