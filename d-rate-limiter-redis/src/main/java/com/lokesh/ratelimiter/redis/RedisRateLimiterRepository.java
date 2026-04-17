package com.lokesh.ratelimiter.redis;

import com.lokesh.ratelimiter.core.model.RateLimitConfig;
import com.lokesh.ratelimiter.core.model.RateLimitResult;
import com.lokesh.ratelimiter.core.model.TokenBucket;
import com.lokesh.ratelimiter.core.port.RateLimiterRepository;
import com.lokesh.ratelimiter.core.support.RateLimitKey;
import com.lokesh.ratelimiter.redis.support.LuaScriptLoader;
import io.lettuce.core.RedisNoScriptException;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of RateLimiterRepository using Redis and Lua Scripts.
 * 
 * Performance & Atomicity Features:
 * - Atomic Multi-Plan Evaluation: Utilizes a single Lua script to evaluate all chained limits
 *   (e.g., 10/sec and 1000/hour) in one network round-trip, preventing "partial success" scenarios.
 * - EVALSHA Optimization: Pre-calculates and uses script SHA1 hashes to minimize network bandwidth
 *   and avoid the overhead of re-parsing Lua code on every request.
 * - Self-Healing: Automatically falls back to EVAL and re-loads the script if Redis loses its 
 *   script cache (NOSCRIPT error).
 * - Fail-Open Resilience: Propagates Redis exceptions to the core orchestrator, which defaults 
 *   to allowing traffic to ensure system availability during infrastructure outages.
 */
public class RedisRateLimiterRepository implements RateLimiterRepository {

    private final StatefulRedisConnection<String, String> connection;
    private final LuaScriptLoader scriptLoader;
    private final String scriptPath = "lua/acquire_token.lua";
    private final String scriptSha;

    public RedisRateLimiterRepository(StatefulRedisConnection<String, String> connection, LuaScriptLoader scriptLoader) {
        this.connection = connection;
        this.scriptLoader = scriptLoader;
        // Pre-calculate SHA at startup for EVALSHA performance
        this.scriptSha = scriptLoader.getSha(scriptPath);
    }

    @Override
    public RateLimitResult tryAcquire(String key, List<RateLimitConfig> configs, int tokensToConsume) {
        if (configs == null || configs.isEmpty()) {
            return RateLimitResult.allow(-1.0);
        }

        // Build alternating KEYS array: [bucket1, config1, bucket2, config2, ...]
        String[] keys = new String[configs.size() * 2];
        for (int i = 0; i < configs.size(); i++) {
            RateLimitConfig config = configs.get(i);
            
            // Bucket Key (State) -> e.g. ratelimiter:default:user_123:gold
            keys[i * 2] = RateLimitKey.builder()
                .withUser(key)
                .withPlan(config.planName())
                .build();
                
            // Config Key (Rules) -> e.g. config:plan:gold
            keys[i * 2 + 1] = "config:plan:" + config.planName();
        }

        String[] args = { String.valueOf(tokensToConsume) };
        RedisCommands<String, String> sync = connection.sync();

        List<Object> result;
        try {
            // Attempt fast path using the cached script SHA
            result = sync.evalsha(scriptSha, ScriptOutputType.MULTI, keys, args);
        } catch (RedisNoScriptException e) {
            // Fallback: If Redis was restarted and lost the script cache, send the full script
            String scriptContent = scriptLoader.getScript(scriptPath);
            result = sync.eval(scriptContent, ScriptOutputType.MULTI, keys, args);
        } catch (Exception e) {
            // Fail-Open trigger: Any other Redis exception throws to be caught by DefaultRateLimiter
            throw new RuntimeException("Redis execution failed", e);
        }

        return parseResult(result);
    }

    @Override
    public Optional<TokenBucket> getState(String key) {
        // key here is expected to be the fully qualified bucket key
        List<io.lettuce.core.KeyValue<String, String>> state = connection.sync().hmget(key, "t", "ts");
        
        if (state == null || state.size() != 2 || !state.get(0).hasValue() || !state.get(1).hasValue()) {
            return Optional.empty();
        }

        try {
            double tokens = Double.parseDouble(state.get(0).getValue());
            long lastRefill = Long.parseLong(state.get(1).getValue());
            return Optional.of(new TokenBucket(tokens, lastRefill));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private RateLimitResult parseResult(List<Object> result) {
        if (result == null || result.size() < 3) {
            throw new IllegalStateException("Invalid Lua script response format");
        }

        long allowedFlag = (Long) result.get(0);
        double remainingTokens = ((Number) result.get(1)).doubleValue();
        long waitMillis = ((Number) result.get(2)).longValue();

        if (allowedFlag == 1L) {
            return RateLimitResult.allow(remainingTokens);
        } else {
            return RateLimitResult.deny(remainingTokens, waitMillis, "RATE_LIMITED");
        }
    }
}