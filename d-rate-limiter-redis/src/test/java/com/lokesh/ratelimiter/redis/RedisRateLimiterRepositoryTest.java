package com.lokesh.ratelimiter.redis;

import com.lokesh.ratelimiter.core.model.RateLimitConfig;
import com.lokesh.ratelimiter.core.model.RateLimitResult;
import com.lokesh.ratelimiter.core.model.TokenBucket;
import com.lokesh.ratelimiter.redis.support.LuaScriptLoader;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisNoScriptException;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterRepositoryTest {

    @Mock
    private StatefulRedisConnection<String, String> connection;

    @Mock
    private RedisCommands<String, String> syncCommands;

    @Mock
    private LuaScriptLoader scriptLoader;

    private RedisRateLimiterRepository repository;

    @BeforeEach
    void setUp() {
        when(scriptLoader.getSha("lua/acquire_token.lua")).thenReturn("fake-sha");
        repository = new RedisRateLimiterRepository(connection, scriptLoader);
    }

    @Test
    void shouldAllowWhenConfigsAreEmpty() {
        RateLimitResult result = repository.tryAcquire("user1", List.of(), 1);
        assertThat(result.allowed()).isTrue();
        assertThat(result.remainingTokens()).isEqualTo(-1.0);
    }

    @Test
    void shouldMapSinglePlanAllowResult() {
        when(connection.sync()).thenReturn(syncCommands);
        RateLimitConfig config = new RateLimitConfig("gold", 100, 10.0);
        
        // Mock EVALSHA returning [1L, 9.0, 0L] (Allowed)
        when(syncCommands.evalsha(eq("fake-sha"), eq(ScriptOutputType.MULTI), any(String[].class), any(String[].class)))
            .thenReturn(List.of(1L, 9.0, 0L));

        RateLimitResult result = repository.tryAcquire("user1", List.of(config), 1);

        assertThat(result.allowed()).isTrue();
        assertThat(result.remainingTokens()).isEqualTo(9.0);
        assertThat(result.waitMillis()).isEqualTo(0L);
    }

    @Test
    void shouldMapMultiPlanAllowResult() {
        when(connection.sync()).thenReturn(syncCommands);
        RateLimitConfig gold = new RateLimitConfig("gold", 100, 10.0);
        RateLimitConfig minute = new RateLimitConfig("minute", 50, 1.0);
        
        // Mock EVALSHA returning [1L, 5.0, 0L] (Allowed)
        when(syncCommands.evalsha(eq("fake-sha"), eq(ScriptOutputType.MULTI), any(String[].class), any(String[].class)))
            .thenReturn(List.of(1L, 5.0, 0L));

        RateLimitResult result = repository.tryAcquire("user1", List.of(gold, minute), 1);

        assertThat(result.allowed()).isTrue();
        assertThat(result.remainingTokens()).isEqualTo(5.0);
        assertThat(result.waitMillis()).isEqualTo(0L);
        
        // Verify Keys formatting
        String[] expectedKeys = {
            "ratelimiter:default:user1:gold", "config:plan:gold",
            "ratelimiter:default:user1:minute", "config:plan:minute"
        };
        verify(syncCommands).evalsha(eq("fake-sha"), eq(ScriptOutputType.MULTI), eq(expectedKeys), eq(new String[]{"1"}));
    }

    @Test
    void shouldMapMultiPlanDenyResult() {
        when(connection.sync()).thenReturn(syncCommands);
        RateLimitConfig gold = new RateLimitConfig("gold", 100, 10.0);
        RateLimitConfig minute = new RateLimitConfig("minute", 50, 1.0);
        
        // Mock EVALSHA returning [0L, 0.0, 500L] (Denied)
        when(syncCommands.evalsha(eq("fake-sha"), eq(ScriptOutputType.MULTI), any(String[].class), any(String[].class)))
            .thenReturn(List.of(0L, 0.0, 500L));

        RateLimitResult result = repository.tryAcquire("user1", List.of(gold, minute), 1);

        assertThat(result.allowed()).isFalse();
        assertThat(result.remainingTokens()).isEqualTo(0.0);
        assertThat(result.waitMillis()).isEqualTo(500L);
        assertThat(result.reason()).isEqualTo("RATE_LIMITED");
        
        // Verify Keys formatting
        String[] expectedKeys = {
            "ratelimiter:default:user1:gold", "config:plan:gold",
            "ratelimiter:default:user1:minute", "config:plan:minute"
        };
        verify(syncCommands).evalsha(eq("fake-sha"), eq(ScriptOutputType.MULTI), eq(expectedKeys), eq(new String[]{"1"}));
    }

    @Test
    void shouldFallbackToEvalOnNoScriptException() {
        when(connection.sync()).thenReturn(syncCommands);
        RateLimitConfig config = new RateLimitConfig("gold", 100, 10.0);
        
        // Throw NoScriptException on EVALSHA
        when(syncCommands.evalsha(eq("fake-sha"), eq(ScriptOutputType.MULTI), any(String[].class), any(String[].class)))
            .thenThrow(new RedisNoScriptException("NOSCRIPT"));
            
        when(scriptLoader.getScript("lua/acquire_token.lua")).thenReturn("local x = 1");
        
        // Succeed on EVAL
        when(syncCommands.eval(eq("local x = 1"), eq(ScriptOutputType.MULTI), any(String[].class), any(String[].class)))
            .thenReturn(List.of(1L, 9.0, 0L));

        RateLimitResult result = repository.tryAcquire("user1", List.of(config), 1);

        assertThat(result.allowed()).isTrue();
        verify(syncCommands).eval(anyString(), eq(ScriptOutputType.MULTI), any(String[].class), any(String[].class));
    }

    @Test
    void shouldPropagateExceptionsForFailOpen() {
        when(connection.sync()).thenReturn(syncCommands);
        RateLimitConfig config = new RateLimitConfig("gold", 100, 10.0);
        
        when(syncCommands.evalsha(anyString(), any(), any(String[].class), any(String[].class)))
            .thenThrow(new RuntimeException("Connection Refused"));

        assertThatThrownBy(() -> repository.tryAcquire("user1", List.of(config), 1))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Redis execution failed");
    }

    @Test
    void shouldReturnEmptyStateWhenNotExists() {
        when(connection.sync()).thenReturn(syncCommands);
        when(syncCommands.hmget("ratelimiter:default:user1:gold", "t", "ts")).thenReturn(List.of(KeyValue.empty("t"), KeyValue.empty("ts")));

        Optional<TokenBucket> state = repository.getState("ratelimiter:default:user1:gold");
        assertThat(state).isEmpty();
    }

    @Test
    void shouldReturnStateWhenExists() {
        when(connection.sync()).thenReturn(syncCommands);
        when(syncCommands.hmget("ratelimiter:default:user1:gold", "t", "ts"))
            .thenReturn(List.of(KeyValue.just("t", "5.5"), KeyValue.just("ts", "1000")));

        Optional<TokenBucket> state = repository.getState("ratelimiter:default:user1:gold");
        assertThat(state).isPresent();
        assertThat(state.get().tokens()).isEqualTo(5.5);
        assertThat(state.get().lastRefillMillis()).isEqualTo(1000L);
    }
}