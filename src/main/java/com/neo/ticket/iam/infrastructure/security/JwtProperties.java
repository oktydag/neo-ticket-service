package com.neo.ticket.iam.infrastructure.security;

import com.neo.ticket.shared.domain.Invariants;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "neo.security.jwt")
public record JwtProperties(String secret, String issuer,
                            Duration accessTokenTtl, Duration refreshTokenTtl) {

    public static final int MIN_SECRET_LENGTH = 32;

    public static final String DEVELOPMENT_SECRET =
            "dev-only-neo-ticket-secret-change-me-before-deploying";

    public JwtProperties {
        Invariants.requirePresent(secret, "neo.security.jwt.secret");
        Invariants.require(secret.length() >= MIN_SECRET_LENGTH,
                "neo.security.jwt.secret must be at least %d characters".formatted(MIN_SECRET_LENGTH));
        Invariants.requireText(issuer, "neo.security.jwt.issuer", 1, 200);
        Invariants.requirePresent(accessTokenTtl, "neo.security.jwt.access-token-ttl");
        Invariants.requirePresent(refreshTokenTtl, "neo.security.jwt.refresh-token-ttl");
        Invariants.require(!accessTokenTtl.isNegative() && !accessTokenTtl.isZero(),
                "access token TTL must be positive");
        Invariants.require(refreshTokenTtl.compareTo(accessTokenTtl) > 0,
                "refresh token must outlive the access token it renews");
    }

    public boolean usesDevelopmentSecret() {
        return DEVELOPMENT_SECRET.equals(secret);
    }
}
