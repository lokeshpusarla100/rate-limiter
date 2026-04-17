package com.lokesh.ratelimiter.redis;

import com.lokesh.ratelimiter.core.model.RateLimitConfig;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisConfigBootstrapperTest {

    @Mock
    private StatefulRedisConnection<String, String> connection;

    @Mock
    private RedisCommands<String, String> syncCommands;

    private RedisConfigBootstrapper bootstrapper;

    @BeforeEach
    void setUp() {
        bootstrapper = new RedisConfigBootstrapper(connection);
    }

    @Test
    void shouldBootstrapConfigsToRedisHashes() {
        when(connection.sync()).thenReturn(syncCommands);

        RateLimitConfig gold = new RateLimitConfig("gold", 100, 10.0);
        RateLimitConfig silver = new RateLimitConfig("silver", 50, 5.0);

        bootstrapper.bootstrap(List.of(gold, silver));

        Map<String, String> expectedGoldData = Map.of(
            "capacity", "100",
            "refillRate", "10.0"
        );
        Map<String, String> expectedSilverData = Map.of(
            "capacity", "50",
            "refillRate", "5.0"
        );

        verify(syncCommands).hset(eq("config:plan:gold"), eq(expectedGoldData));
        verify(syncCommands).hset(eq("config:plan:silver"), eq(expectedSilverData));
    }

    @Test
    void shouldDoNothingWhenConfigsListIsEmpty() {
        bootstrapper.bootstrap(List.of());
        verifyNoInteractions(connection);
    }

    @Test
    void shouldDoNothingWhenConfigsListIsNull() {
        bootstrapper.bootstrap(null);
        verifyNoInteractions(connection);
    }
}