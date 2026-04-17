package com.lokesh.ratelimiter.redis;

import com.lokesh.ratelimiter.core.model.RateLimitConfig;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.List;
import java.util.Map;

/**
 * Bootstrapper to synchronize RateLimitConfig objects into Redis.
 *
 * This component is critical for the "Hot Reloading" architecture (ADR-005):
 * 1. At startup, it pushes Java-defined plans (e.g., from application.yml) into Redis Hashes.
 * 2. The Lua script reads directly from these Redis hashes on every request.
 * 3. This allows operational teams to modify limits directly in Redis to react to traffic 
 *    spikes instantly without restarting the application cluster.
 * 4. It ensures data consistency between the Java PlanRegistry and the Redis-native Lua script.
 */
public class RedisConfigBootstrapper {

    private final StatefulRedisConnection<String, String> connection;

    public RedisConfigBootstrapper(StatefulRedisConnection<String, String> connection) {
        this.connection = connection;
    }

    /**
     * Synchronizes a list of plan configurations into Redis Hashes.
     * 
     * @param configs the rate limit configurations to persist.
     */
    public void bootstrap(List<RateLimitConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return;
        }

        RedisCommands<String, String> sync = connection.sync();

        for (RateLimitConfig config : configs) {
            String configKey = "config:plan:" + config.planName();
            
            Map<String, String> data = Map.of(
                "capacity", String.valueOf(config.capacity()),
                "refillRate", String.valueOf(config.tokensPerSecond())
            );
            
            sync.hset(configKey, data);
        }
    }
}