package com.lokesh.ratelimiter.core.port;

import com.lokesh.ratelimiter.core.model.RateLimitResult;
import java.util.List;

/**
 * Outbound Port: Interface for receiving rate-limiting lifecycle events.
 * 
 * <p>Architectural Role: <b>Observability SPI</b>.
 * Implementations of this interface (Adapters) allow the system to export metrics,
 * produce structured logs, or trigger alerts without coupling the Core to 
 * infrastructure like Micrometer or Prometheus. [Fix 7]
 */
public interface RateLimitEventListener {
    
    /**
     * Triggered when a request passes all rate limit checks.
     */
    default void onAllow(String key, List<String> plans, RateLimitResult result) {}
    
    /**
     * Triggered when a request is blocked by one or more plans.
     */
    default void onDeny(String key, List<String> plans, RateLimitResult result) {}
    
    /**
     * Triggered when an infrastructure failure occurs and the system fails-open.
     */
    default void onFailOpen(String key, String reason) {}
    
    /**
     * Triggered when a requested plan name cannot be found in the registry.
     */
    default void onPlanMissing(String planName) {}
}