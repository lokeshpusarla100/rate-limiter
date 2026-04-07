package com.lokesh.ratelimiter.core.support;

import com.lokesh.ratelimiter.core.port.KeyResolver;
import com.lokesh.ratelimiter.core.port.RequestSource;

/**
 * Strategy for resolving a rate-limiting key based on the authenticated principal.
 * 
 * Key Resolution Strategy.
 * Used for per-user rate limiting in authenticated environments.
 * 
 * Implementation Details:
 *   - Anonymous Fallback: If no principal is found, it defaults to 
 *       "anonymous" to ensure a consistent key.
 */
public class PrincipalKeyResolver implements KeyResolver {

    @Override
    public String resolve(RequestSource source) {
        String principal = source.getPrincipalName();
        return principal != null ? principal : "anonymous";
    }
}