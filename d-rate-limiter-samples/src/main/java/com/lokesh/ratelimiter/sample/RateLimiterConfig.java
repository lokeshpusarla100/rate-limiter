package com.lokesh.ratelimiter.sample;

import com.lokesh.ratelimiter.core.model.RateLimitConfig;
import com.lokesh.ratelimiter.core.port.PlanRegistry;
import com.lokesh.ratelimiter.core.port.RateLimiter;
import com.lokesh.ratelimiter.core.port.RateLimiterRepository;
import com.lokesh.ratelimiter.core.service.DefaultRateLimiter;
import com.lokesh.ratelimiter.core.support.InMemoryPlanRegistry;
import com.lokesh.ratelimiter.redis.RedisConfigBootstrapper;
import com.lokesh.ratelimiter.redis.RedisRateLimiterRepository;
import com.lokesh.ratelimiter.redis.support.LuaScriptLoader;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RateLimiterConfig {

    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient(@Value("${spring.redis.url:redis://localhost:6379}") String redisUrl) {
        return RedisClient.create(redisUrl);
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, String> redisConnection(RedisClient redisClient) {
        return redisClient.connect();
    }

    @Bean
    public LuaScriptLoader luaScriptLoader() {
        return new LuaScriptLoader();
    }

    @Bean
    public RateLimiterRepository rateLimiterRepository(StatefulRedisConnection<String, String> connection, LuaScriptLoader loader) {
        return new RedisRateLimiterRepository(connection, loader);
    }

    @Bean
    public PlanRegistry planRegistry() {
        InMemoryPlanRegistry registry = new InMemoryPlanRegistry();
        // Define a "benchmark" plan: 100 capacity, 100 tokens per second
        registry.registerPlan(new RateLimitConfig("benchmark", 100, 100.0));
        return registry;
    }

    @Bean
    public RedisConfigBootstrapper redisConfigBootstrapper(StatefulRedisConnection<String, String> connection, PlanRegistry planRegistry) {
        RedisConfigBootstrapper bootstrapper = new RedisConfigBootstrapper(connection);
        
        RateLimitConfig benchmarkPlan = planRegistry.getPlan("benchmark").orElseThrow();
        bootstrapper.bootstrap(List.of(benchmarkPlan));
        
        return bootstrapper;
    }

    @Bean
    public RateLimiter rateLimiter(RateLimiterRepository repository, PlanRegistry registry, RedisConfigBootstrapper bootstrapper) {
        // Bootstrapper is injected to ensure it runs before the RateLimiter is created
        return new DefaultRateLimiter(repository, registry);
    }
}