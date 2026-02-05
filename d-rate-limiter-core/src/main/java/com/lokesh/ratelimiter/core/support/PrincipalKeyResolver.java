package com.lokesh.ratelimiter.core.support;

import com.lokesh.ratelimiter.core.port.KeyResolver;
import com.lokesh.ratelimiter.core.port.RequestSource;

/**
 * Strategy for resolving a rate-limiting key based on the authenticated principal.
 * 
 * <p>Architectural Role: <b>Key Resolution Strategy</b>.
 * Used for per-user rate limiting in authenticated environments.
 * 
 * <p>Implementation Details:
 * <ul>
 *   <li><b>Anonymous Fallback</b>: If no principal is found, it defaults to 
 *       "anonymous" to ensure a consistent key.</li>
 * </ul>
 */
public class PrincipalKeyResolver implements KeyResolver {

    @Override
    public String resolve(RequestSource source) {
        String principal = source.getPrincipalName();
        return principal != null ? principal : "anonymous";
    }
}