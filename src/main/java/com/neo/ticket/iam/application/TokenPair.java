package com.neo.ticket.iam.application;

import java.time.Duration;
import java.time.Instant;

public record TokenPair(String accessToken, String refreshToken, String tokenType,
                        long expiresIn, Instant refreshTokenExpiresAt) {

    public static final String BEARER = "Bearer";

    public static TokenPair of(TokenIssuer.IssuedToken accessToken,
                               TokenIssuer.IssuedToken refreshToken,
                               Instant now) {
        return new TokenPair(
                accessToken.value(),
                refreshToken.value(),
                BEARER,
                Duration.between(now, accessToken.expiresAt()).toSeconds(),
                refreshToken.expiresAt());
    }
}
