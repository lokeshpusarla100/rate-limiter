package com.lokesh.ratelimiter.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Default implementation of the {@link RateLimiter} port.
 *
 * <p>Architectural Alignment:
 * <ul>
 *   <li><b>ADR 002 (Defensive Programming)</b>: Implements the Fail-Open principle.
 *       If the repository throws an exception (e.g., Redis timeout), the error is logged,
 *       and the request is allowed to ensure system availability.</li>
 *   <li><b>ADR 001 (Hexagonal)</b>: Pure Java implementation with no external infra dependencies.</li>
 * </ul>
 */
public class DefaultRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(DefaultRateLimiter.class);

    private final RateLimiterRepository repository;

    /**
     * Constructs a new DefaultRateLimiter.
     * 
     * @param repository The outbound port for state persistence (Fail-Fast on null).
     */
    public DefaultRateLimiter(RateLimiterRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository must not be null");
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation delegates to the repository and catches all exceptions
     * to perform a "Fail-Open" operation as per ADR 002.
     */
    @Override
    public boolean allow(String key, RateLimitConfig config) {
        try {
            // Using System.nanoTime() as the reference for refill calculations.
            return repository.tryAcquire(key, config, System.nanoTime());
        } catch (Exception e) {
            log.error("Rate limiter failure for key [{}]. Defaulting to FAIL-OPEN (ALLOW). Reason: {}", 
                    key, e.getMessage(), e);
            return true; 
        }
    }
}
