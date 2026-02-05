package com.lokesh.ratelimiter.core.port;

import com.lokesh.ratelimiter.core.model.RateLimitResult;
import java.util.List;

/**
 * Port: Outbound interface for observability.
 * [Fix 7] Enables plugging in metrics and logging without modifying core.
 */
public interface RateLimitEventListener {
    
    void onAllow(String key, List<String> plans, RateLimitResult result);
    
    void onDeny(String key, List<String> plans, RateLimitResult result);
    
    void onFailOpen(String key, String reason);
    
    void onPlanMissing(String planName);
}
