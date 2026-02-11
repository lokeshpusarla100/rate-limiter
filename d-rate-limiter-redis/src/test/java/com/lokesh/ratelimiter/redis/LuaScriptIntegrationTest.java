package com.lokesh.ratelimiter.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify Lua script execution in a real Redis environment.
 * Adheres to ADR-003 (TDD) by establishing a failing state for rate limiting logic.
 */
@Testcontainers
class LuaScriptIntegrationTest {

    /**
     * The @Container annotation tells the Testcontainers extension to start/stop this 
     * Redis instance automatically. Manual close/try-with-resources is not required.
     */
    @Container
    private static final GenericContainer<?> REDIS = 
        new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    private static RedisClient redisClient;
    private static StatefulRedisConnection<String, String> connection;

    /**
     * Initializes the Direct Lettuce connection before any tests run.
     */
    @BeforeAll
    static void setup() {
        // Construct the Redis URL dynamically based on the container's mapped port
        String redisUrl = String.format("redis://%s:%d", 
            REDIS.getHost(), REDIS.getFirstMappedPort());
        
        // Create the client (The "Seed")
        redisClient = RedisClient.create(redisUrl);
        connection = redisClient.connect();
    }

    /**
     * Clean up resources after all tests have finished.
     */
    @AfterAll
    static void tearDown() {
        if (connection != null) connection.close();
        if (redisClient != null) redisClient.shutdown();
    }

    /**
     * Verifies that the connection is alive and basic Lua execution works.
     */
    @Test
    void shouldExecuteBasicLuaScript() {
        // Simple script: just return the first argument (ARGV[1])
        String script = "return ARGV[1]";
        
        // Execute eval directly on the synchronous API
        String result = connection.sync().eval(
            script, 
            ScriptOutputType.VALUE, 
            new String[0], 
            "Hello Lua"
        );
        
        assertThat(result).isEqualTo("Hello Lua");
    }

    @Test
    void shouldAllowInitialTokenAcquisitionWithConfig() {
        // --- GIVEN ---
        String bucketKey = "ratelimiter:test:user_1:gold";
        String configKey = "config:plan:gold"; // Key to store rate limit configuration

        // Pre-populate Redis with configuration for the "gold" plan
        connection.sync().hset(configKey, "capacity", "10.0");
        connection.sync().hset(configKey, "refillRate", "1.0"); // 1 token per second

        // Ensure clean state for the bucket
        connection.sync().del(bucketKey);

        int tokensToConsume = 1;
        String scriptContent = loadScript("lua/acquire_token.lua");

        // --- WHEN ---
        // Execute script with bucketKey, configKey as KEYS and tokensToConsume as ARGV
        List<Object> result = connection.sync().eval(
            scriptContent,
            ScriptOutputType.MULTI,
            new String[]{bucketKey, configKey}, // KEYS[1]: bucketKey, KEYS[2]: configKey
            String.valueOf(tokensToConsume)       // ARGV[1]: tokensToConsume
        );

        // --- THEN ---
        // Verify the result matches our expectations for a success scenario
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(1L);      // Allowed (1 for true)
        
        // Use doubleValue() for flexible comparison as Redis might return Long for whole numbers
        assertThat(((Number) result.get(1)).doubleValue()).isEqualTo(9.0); 
        
        assertThat(result.get(2)).isEqualTo(0L);      // Wait time in millis
    }

    @Test
    void shouldDenyTokenAcquisitionWhenBucketIsEmpty() {
        // --- GIVEN ---
        String bucketKey = "ratelimiter:test:user_2:gold";
        String configKey = "config:plan:gold";

        connection.sync().hset(configKey, "capacity", "10.0");
        connection.sync().hset(configKey, "refillRate", "1.0");

        // Manually set bucket to empty state (0 tokens) with a very recent timestamp
        // We use the current time from Redis to be consistent with the script
        List<String> time = connection.sync().time();
        long nowMs = (Long.parseLong(time.get(0)) * 1000) + (Long.parseLong(time.get(1)) / 1000);
        
        connection.sync().hset(bucketKey, "t", "0.0");
        connection.sync().hset(bucketKey, "ts", String.valueOf(nowMs));

        int tokensToConsume = 1;
        String scriptContent = loadScript("lua/acquire_token.lua");

        // --- WHEN ---
        List<Object> result = connection.sync().eval(
            scriptContent,
            ScriptOutputType.MULTI,
            new String[]{bucketKey, configKey},
            String.valueOf(tokensToConsume)
        );

        // --- THEN ---
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(0L); // Denied (0 for false)
        
        // Remaining should be near 0 (might have refilled a tiny bit between hset and eval)
        assertThat(((Number) result.get(1)).doubleValue()).isLessThan(1.0);
        
        // Wait time should be calculated (to get 1 token at 1 token/sec, it's ~1000ms)
        assertThat((Long) result.get(2)).isGreaterThan(0L);
    }

    /**
     * Helper to load Lua script from classpath.
     */
    private String loadScript(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Lua script not found on classpath: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Lua script from classpath: " + path, e);
        }
    }
}