package com.lokesh.ratelimiter.core.support;

import com.lokesh.ratelimiter.core.port.KeyResolver;
import com.lokesh.ratelimiter.core.port.RequestSource;

import java.util.Objects;

/**
 * Strategy for resolving a rate-limiting key based on a specific HTTP/Request header.
 * 
 * <p>Architectural Role: <b>Key Resolution Strategy</b>.
 * Commonly used for identifying clients via API Keys (e.g., "X-API-KEY") or 
 * authentication tokens.
 * 
 * <p>Implementation Details:
 * <ul>
 *   <li><b>Fail-Fast</b>: Validates that the header name is not null at construction.</li>
 *   <li><b>Empty Default</b>: Returns an empty string if the header is missing, 
 *       ensuring the key is never null.</li>
 * </ul>
 */
public class HeaderKeyResolver implements KeyResolver {

    private final String headerName;

    public HeaderKeyResolver(String headerName) {
        this.headerName = Objects.requireNonNull(headerName, "headerName must not be null");
    }

    @Override
    public String resolve(RequestSource source) {
        String value = source.getHeader(headerName);
        return value != null ? value : "";
    }
}