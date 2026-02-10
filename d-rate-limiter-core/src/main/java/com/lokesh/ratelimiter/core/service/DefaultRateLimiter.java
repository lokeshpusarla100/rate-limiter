package com.lokesh.ratelimiter.core.service;

import com.lokesh.ratelimiter.core.model.RateLimitConfig;
import com.lokesh.ratelimiter.core.model.RateLimitResult;
import com.lokesh.ratelimiter.core.port.PlanRegistry;
import com.lokesh.ratelimiter.core.port.RateLimiter;
import com.lokesh.ratelimiter.core.port.RateLimiterRepository;
import com.lokesh.ratelimiter.core.port.RateLimitEventListener;
import com.lokesh.ratelimiter.core.support.MissingPlanPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of the {@link RateLimiter} port.
 * 
 * <p>
 * Responsibilities:
 * <ul>
 * <li><b>Orchestration (Gap 8)</b>: Resolves plan names into concrete
 * configurations.</li>
 * <li><b>Resilience (ADR 002)</b>: Implements the Fail-Open principle.</li>
 * <li><b>Observability (Fix 7)</b>: Notifies listeners of all rate-limiting
 * events.</li>
 * <li><b>Security (Fix 2)</b>: Enforces plan resolution policies.</li>
 * </ul>
 */
public class DefaultRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(DefaultRateLimiter.class);

    private final RateLimiterRepository repository;
    private final PlanRegistry planRegistry;
    private final List<RateLimitEventListener> listeners;
    private final MissingPlanPolicy missingPlanPolicy;

    /**
     * Convenience constructor using sensible defaults: no listeners,
     * {@link MissingPlanPolicy#FAIL_FAST}.
     *
     * @param repository   the driven port for atomic bucket operations.
     * @param planRegistry the registry from which plan configs are resolved.
     */
    public DefaultRateLimiter(RateLimiterRepository repository, PlanRegistry planRegistry) {
        this(repository, planRegistry, Collections.emptyList(), MissingPlanPolicy.FAIL_FAST);
    }

    /**
     * Full constructor.
     *
     * @param repository        the driven port for atomic bucket operations.
     * @param planRegistry      the registry from which plan configs are resolved.
     * @param listeners         zero or more observers for allow/deny/fail-open
     *                          events.
     * @param missingPlanPolicy how to react when a requested plan is not found.
     * @throws NullPointerException if any argument is {@code null}.
     */
    public DefaultRateLimiter(RateLimiterRepository repository,
            PlanRegistry planRegistry,
            List<RateLimitEventListener> listeners,
            MissingPlanPolicy missingPlanPolicy) {
        this.repository = Objects.requireNonNull(repository, "Repository must not be null");
        this.planRegistry = Objects.requireNonNull(planRegistry, "PlanRegistry must not be null");
        this.listeners = Objects.requireNonNull(listeners, "Listeners must not be null");
        this.missingPlanPolicy = Objects.requireNonNull(missingPlanPolicy, "MissingPlanPolicy must not be null");
    }

    @Override
    public RateLimitResult allow(String key, List<String> planNames, int tokensToConsume) {
        // 1. Resolve Plans (Logic/Config - Should Fail-Fast if configured)
        List<RateLimitConfig> configs = new ArrayList<>();
        for (String planName : planNames) {
            Optional<RateLimitConfig> config = planRegistry.getPlan(planName);
            if (config.isEmpty()) {
                handleMissingPlan(planName, key); // May throw IllegalArgumentException
                continue;
            }
            configs.add(config.get());
        }

        // Security check: REQUIRE_AT_LEAST_ONE policy
        if (configs.isEmpty() && missingPlanPolicy == MissingPlanPolicy.REQUIRE_AT_LEAST_ONE && !planNames.isEmpty()) {
            throw new IllegalStateException(
                    "No valid plans found for key [" + key + "] and policy REQUIRE_AT_LEAST_ONE");
        }

        // 2. Execute Check (Infrastructure - Should Fail-Open)
        try {
            if (configs.isEmpty()) {
                RateLimitResult res = RateLimitResult.allow(-1);
                notifyAllow(key, planNames, res);
                return res;
            }

            RateLimitResult result = repository.tryAcquire(key, configs, tokensToConsume);

            if (result.allowed()) {
                notifyAllow(key, planNames, result);
            } else {
                notifyDeny(key, planNames, result);
            }

            return result;
        } catch (Exception e) {
            // FAIL-OPEN (ADR 002): Never block the user due to infrastructure errors.
            log.error("Rate limiter infrastructure failure for key [{}]. Defaulting to FAIL-OPEN. Reason: {}",
                    key, e.getMessage(), e);
            notifyFailOpen(key, e.getMessage());
            return RateLimitResult.failOpen(e.getMessage());
        }
    }

    /**
     * Handles a missing plan by notifying listeners and then either throwing
     * (FAIL_FAST) or logging a warning (SKIP_WITH_WARN / REQUIRE_AT_LEAST_ONE).
     */
    private void handleMissingPlan(String planName, String key) {
        listeners.forEach(l -> l.onPlanMissing(planName));
        if (missingPlanPolicy == MissingPlanPolicy.FAIL_FAST) {
            throw new IllegalArgumentException("Rate limit plan [" + planName + "] not found for key [" + key + "]");
        }
        log.warn("Rate limit plan [{}] not found for key [{}]. Check configuration.", planName, key);
    }

    /** Broadcasts an allow event to all registered listeners. */
    private void notifyAllow(String key, List<String> plans, RateLimitResult result) {
        listeners.forEach(l -> l.onAllow(key, plans, result));
    }

    /** Broadcasts a deny event to all registered listeners. */
    private void notifyDeny(String key, List<String> plans, RateLimitResult result) {
        listeners.forEach(l -> l.onDeny(key, plans, result));
    }

    /** Broadcasts a fail-open event to all registered listeners. */
    private void notifyFailOpen(String key, String reason) {
        listeners.forEach(l -> l.onFailOpen(key, reason));
    }
}
