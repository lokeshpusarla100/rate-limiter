package com.lokesh.ratelimiter.core.port;

/**
 * Port: Abstraction of an incoming request source.
 *
 * <p>Architectural Alignment:
 * <ul>
 *   <li><b>ADR 001 (Hexagonal Architecture)</b>: This interface ensures the Core module
 *       remains "Pure Java" by abstracting away framework-specific classes like
 *       {@code HttpServletRequest} or {@code ServerWebExchange}.</li>
 *   <li><b>ADR 005 (Tenancy)</b>: Provides the metadata necessary for {@link KeyResolver}
 *       implementations to identify the requester (e.g., via IP or Headers).</li>
 * </ul>
 *
 * <p>Implementations of this port (Adapters) will live in infrastructure modules
 * (e.g., {@code d-rate-limiter-spring-boot-starter}).
 */
public interface RequestSource {

    /**
     * Retrieves the network address of the requester.
     *
     * @return The IP address or remote identifier as a String.
     */
    String getRemoteAddress();

    /**
     * Retrieves the value of a specific request header.
     * Useful for resolving keys based on API Keys or JWT claims.
     *
     * @param name The name of the header.
     * @return The header value, or {@code null} if not present.
     */
    String getHeader(String name);

    /**
     * Retrieves the name of the authenticated user/principal.
     *
     * @return The principal name, or {@code null} if the request is anonymous.
     */
    String getPrincipalName();
}