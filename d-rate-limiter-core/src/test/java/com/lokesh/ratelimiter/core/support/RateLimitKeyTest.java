package com.lokesh.ratelimiter.core.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RateLimitKeyTest {

    @Test
    @DisplayName("[Fix 8] Should build a key with the standard format")
    void shouldBuildStandardKey() {
        // GIVEN
        String expected = "ratelimiter:my-tenant:user-1:gold";

        // WHEN
        String actual = RateLimitKey.builder()
                .withTenant("my-tenant")
                .withUser("user-1")
                .withPlan("gold")
                .build();

        // THEN
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("[Fix 8] Should use defaults for missing fields")
    void shouldUseCaseDefaults() {
        // GIVEN
        String expected = "ratelimiter:default:anonymous:global";

        // WHEN
        String actual = RateLimitKey.builder().build();

        // THEN
        assertThat(actual).isEqualTo(expected);
    }
}
