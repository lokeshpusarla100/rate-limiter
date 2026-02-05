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
class PrincipalKeyResolverTest {

    @Mock
    private RequestSource requestSource;

    private final PrincipalKeyResolver resolver = new PrincipalKeyResolver();

    @Test
    @DisplayName("Should resolve key from principal name when authenticated")
    void shouldResolveKeyFromPrincipal() {
        // GIVEN
        when(requestSource.getPrincipalName()).thenReturn("user-lokesh");

        // WHEN
        String key = resolver.resolve(requestSource);

        // THEN
        assertThat(key).isEqualTo("user-lokesh");
    }

    @Test
    @DisplayName("Should return 'anonymous' when principal is missing")
    void shouldReturnAnonymousWhenPrincipalMissing() {
        // GIVEN
        when(requestSource.getPrincipalName()).thenReturn(null);

        // WHEN
        String key = resolver.resolve(requestSource);

        // THEN
        assertThat(key).isEqualTo("anonymous");
    }
}
