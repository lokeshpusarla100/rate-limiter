package com.lokesh.ratelimiter.core.port;

/**
 * Port: Strategy for resolving a unique rate-limiting key from a request.
 *
 * <p>Architectural Alignment:
 * <ul>
 *   <li><b>ADR 005 (Key Resolvers)</b>: Decouples "Who" is being limited from "How" is limited.
 *       Allows users to provide custom strategies for identifying clients (e.g., by IP, User ID, or API Key).</li>
 * </ul>
 * 
 * <p><b>Concurrency Contract [Fix 12]</b>: Implementations MUST be thread-safe.
 * Resolvers are stateless or immutable by design to handle high concurrency.
 */
@FunctionalInterface
public interface KeyResolver {

    /**
     * Resolves a unique string identifier from the given request source.
     *
     * @param source The abstracted request metadata (see {@link RequestSource}).
     * @return A unique key representing the identity to be rate-limited.
     *         Must not return null; an empty string or default value is preferred for anonymous users.
     */
    String resolve(RequestSource source);
}