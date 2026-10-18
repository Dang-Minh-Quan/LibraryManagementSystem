package com.library_management.security;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Base64;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigurationTest {
    private final JwtConfiguration configuration = new JwtConfiguration();

    @Test
    void refusesWeakOrMalformedSecret() {
        assertThatThrownBy(() -> configuration.jwtSigningKey(new JwtProperties("invalid!", "test", Duration.ofHours(1))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Base64");
        assertThatThrownBy(() -> configuration.jwtSigningKey(new JwtProperties("c2hvcnQ=", "test", Duration.ofHours(1))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("32 decoded bytes");
    }

    @Test
    void refusesNonPositiveLifetime() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        for (Duration duration : new Duration[]{Duration.ZERO, Duration.ofSeconds(-1)}) {
            assertThatThrownBy(() -> configuration.jwtSigningKey(new JwtProperties(key, "test", duration)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("positive");
        }
    }
}
