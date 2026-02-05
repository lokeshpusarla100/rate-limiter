package com.lokesh.ratelimiter.core.service;

import com.lokesh.ratelimiter.core.model.RateLimitConfig;
import com.lokesh.ratelimiter.core.model.RateLimitResult;
import com.lokesh.ratelimiter.core.port.PlanRegistry;
import com.lokesh.ratelimiter.core.port.RateLimiter;
import com.lokesh.ratelimiter.core.port.RateLimiterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of the {@link RateLimiter} port.
 * 
 * <p>Architectural Role: <b>Domain Service (Orchestrator)</b>.
 * This class serves as the brain of the rate limiting process. It coordinates 
 * between the configuration registry and the persistence repository.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li><b>Orchestration (Gap 8)</b>: Resolves plan names into concrete {@link RateLimitConfig} objects.</li>
 *   <li><b>Resilience (ADR 002)</b>: Implements the Fail-Open principle. If the underlying 
 *       storage fails, it permits the request to prevent library-induced downtime.</li>
 *   <li><b>Atomicity (ADR 005)</b>: Passes all relevant configs to the repository 
 *       at once to enable atomic evaluation.</li>
 * </ul>
 */
public class DefaultRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(DefaultRateLimiter.class);

    private final RateLimiterRepository repository;
    private final PlanRegistry planRegistry;

    public DefaultRateLimiter(RateLimiterRepository repository, PlanRegistry planRegistry) {
        this.repository = Objects.requireNonNull(repository, "Repository must not be null");
        this.planRegistry = Objects.requireNonNull(planRegistry, "PlanRegistry must not be null");
    }

    /**
     * {@inheritDoc}
     * 
     * <p>This implementation resolves plan names and executes an atomic repository check.
     * If a plan is not found, it is ignored with a warning. If no valid plans are 
     * provided, the request is allowed.
     */
    @Override
    public RateLimitResult allow(String key, List<String> planNames, int tokensToConsume) {
        try {
            // Resolve plan names to concrete configurations
            List<RateLimitConfig> configs = new ArrayList<>();
            for (String planName : planNames) {
                Optional<RateLimitConfig> config = planRegistry.getPlan(planName);
                if (config.isEmpty()) {
                    log.warn("Rate limit plan [{}] not found for key [{}]. Check your configuration.", planName, key);
                    continue;
                }
                configs.add(config.get());
            }

            // If no plans were matched, we default to allowing the request
            if (configs.isEmpty()) {
                log.debug("No valid rate limit plans found for key [{}]. Allowing request.", key);
                return RateLimitResult.allow(-1);
            }

            // Atomic execution via the repository
            return repository.tryAcquire(key, configs, tokensToConsume);
        } catch (Exception e) {
            // FAIL-OPEN (ADR 002): Never block the user due to internal or infrastructure errors.
            log.error("Rate limiter failure for key [{}]. Defaulting to FAIL-OPEN. Reason: {}", 
                    key, e.getMessage(), e);
            return RateLimitResult.failOpen(e.getMessage());
        }
    }
}