package com.lokesh.ratelimiter.redis.support;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for LuaScriptLoader.
 * Verifies script loading from classpath, SHA1 calculation, and caching behavior.
 */
class LuaScriptLoaderTest {

    /**
     * Verifies that a valid Lua script can be loaded from the classpath.
     */
    @Test
    void shouldLoadScriptFromClasspath() {
        LuaScriptLoader loader = new LuaScriptLoader();
        String script = loader.getScript("lua/acquire_token.lua");
        
        assertThat(script).isNotEmpty();
        // Check for a known comment in the script to verify correct content
        assertThat(script).contains("acquire_token.lua");
    }

    /**
     * Verifies that the SHA1 hash is correctly calculated and cached.
     * Caching is critical for high-performance EVALSHA execution.
     */
    @Test
    void shouldCalculateAndCacheSha1() {
        LuaScriptLoader loader = new LuaScriptLoader();
        String sha = loader.getSha("lua/acquire_token.lua");
        
        assertThat(sha).hasSize(40); // Standard SHA1 hexadecimal length
        
        // Subsequent calls should return the same cached SHA
        assertThat(sha).isEqualTo(loader.getSha("lua/acquire_token.lua"));
    }

    /**
     * Verifies that a descriptive RuntimeException is thrown when a script is missing.
     */
    @Test
    void shouldThrowExceptionWhenScriptNotFound() {
        LuaScriptLoader loader = new LuaScriptLoader();
        assertThatThrownBy(() -> loader.getScript("non-existent.lua"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("not found on classpath");
    }
}
