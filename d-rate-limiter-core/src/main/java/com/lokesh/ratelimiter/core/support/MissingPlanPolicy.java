package com.lokesh.ratelimiter.core.support;

/**
 * Policy defining how the system reacts when a requested rate-limit plan name 
 * cannot be resolved to a concrete configuration.
 * 
 * <p>Architectural Role: <b>Security Configuration</b>. [Fix 2]
 */
public enum MissingPlanPolicy {
    /** 
     * Immediately throw an exception. 
     * Recommended for production to prevent silent bypasses due to typos.
     */
    FAIL_FAST,
    
    /** 
     * Log a warning and skip the plan evaluation. 
     * Useful during development or migration.
     */
    SKIP_WITH_WARN,
    
    /** 
     * Allow the request only if at least one other requested plan is found.
     * If NO plans are resolved, the request is blocked.
     */
    REQUIRE_AT_LEAST_ONE
}