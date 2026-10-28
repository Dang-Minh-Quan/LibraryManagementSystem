package com.library_management.security;

import com.library_management.auth.AccessTokenIssuer;
import com.library_management.user.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class JwtTokenService implements AccessTokenIssuer {
    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public JwtTokenService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    public String issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer()).audience(List.of(JwtConfiguration.AUDIENCE))
                .subject(user.getId().toString()).issuedAt(now).expiresAt(now.plus(properties.accessTokenTtl()))
                .id(UUID.randomUUID().toString()).claim("username", user.getUsername())
                .claim("role", user.getRole().name()).build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
