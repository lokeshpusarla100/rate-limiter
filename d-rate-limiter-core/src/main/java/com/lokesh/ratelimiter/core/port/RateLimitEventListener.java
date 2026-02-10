package com.lokesh.ratelimiter.core.port;

import com.lokesh.ratelimiter.core.model.RateLimitResult;
import java.util.List;

/**
 * Outbound Port: Interface for receiving rate-limiting lifecycle events.
 * 
 * <p>
 * Architectural Role: <b>Observability SPI</b>.
 * Implementations of this interface (Adapters) allow the system to export
 * metrics,
 * produce structured logs, or trigger alerts without coupling the Core to
 * infrastructure like Micrometer or Prometheus. [Fix 7]
 */
public interface RateLimitEventListener {

    /**
     * Triggered when a request passes all rate limit checks.
     *
     * @param key    the identity that was evaluated.
     * @param plans  the plan names that were checked.
     * @param result the allow result with remaining-token metadata.
     */
    default void onAllow(String key, List<String> plans, RateLimitResult result) {
    }

    /**
     * Triggered when a request is blocked by one or more plans.
     *
     * @param key    the identity that was rate-limited.
     * @param plans  the plan names that were checked.
     * @param result the deny result with wait-time and reason metadata.
     */
    default void onDeny(String key, List<String> plans, RateLimitResult result) {
    }

    /**
     * Triggered when an infrastructure failure occurs and the system fails-open.
     *
     * @param key    the identity whose check could not be completed.
     * @param reason a human-readable description of the failure (e.g.,
     *               {@code "Redis timeout"}).
     */
    default void onFailOpen(String key, String reason) {
    }

    /**
     * Triggered when a requested plan name cannot be found in the registry.
     *
     * @param planName the missing plan name.
     */
    default void onPlanMissing(String planName) {
    }
}