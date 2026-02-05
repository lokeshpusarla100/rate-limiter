package com.lokesh.ratelimiter.core.support;

/**
 * [Fix 2] Controls behavior when a requested plan name cannot be resolved.
 */
public enum MissingPlanPolicy {
    /** Immediately throw an exception. */
    FAIL_FAST,
    
    /** Log a warning and skip the plan. */
    SKIP_WITH_WARN,
    
    /** Allow only if at least one other plan is resolved. */
    REQUIRE_AT_LEAST_ONE
}
