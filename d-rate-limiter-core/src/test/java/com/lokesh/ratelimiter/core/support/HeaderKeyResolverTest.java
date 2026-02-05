package com.lokesh.ratelimiter.core.support;

import com.lokesh.ratelimiter.core.port.RequestSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeaderKeyResolverTest {

    @Mock
    private RequestSource requestSource;

    private final HeaderKeyResolver resolver = new HeaderKeyResolver("X-API-KEY");

    @Test
    @DisplayName("Should resolve key from header when present")
    void shouldResolveKeyFromHeader() {
        // GIVEN
        when(requestSource.getHeader("X-API-KEY")).thenReturn("key-123");

        // WHEN
        String key = resolver.resolve(requestSource);

        // THEN
        assertThat(key).isEqualTo("key-123");
    }

    @Test
    @DisplayName("Should return empty string when header is missing")
    void shouldReturnEmptyStringWhenHeaderMissing() {
        // GIVEN
        when(requestSource.getHeader("X-API-KEY")).thenReturn(null);

        // WHEN
        String key = resolver.resolve(requestSource);

        // THEN
        assertThat(key).isEmpty();
    }
}
