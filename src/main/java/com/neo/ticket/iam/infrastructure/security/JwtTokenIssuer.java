package com.neo.ticket.iam.infrastructure.security;

import com.neo.ticket.iam.application.TokenIssuer;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.error.AuthenticationFailedException;
import com.neo.ticket.shared.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
class JwtTokenIssuer implements TokenIssuer {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenIssuer.class);

    private static final MacAlgorithm ALGORITHM = MacAlgorithm.HS256;

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final JwtProperties properties;

    JwtTokenIssuer(JwtEncoder encoder, JwtDecoder decoder, JwtProperties properties) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.properties = properties;
    }

    @Override
    public IssuedToken issueAccessToken(UserId userId, Set<Role> roles, Instant now) {
        Instant expiresAt = now.plus(properties.accessTokenTtl());
        JwtClaimsSet claims = baseClaims(userId, now, expiresAt, UUID.randomUUID())
                .claim(JwtClaims.TOKEN_TYPE, JwtClaims.ACCESS_TOKEN_TYPE)
                .claim(JwtClaims.ROLES, roles.stream().map(Role::name).sorted().toList())
                .build();
        return new IssuedToken(encode(claims), expiresAt);
    }

    @Override
    public IssuedToken issueRefreshToken(UserId userId, UUID tokenId, Instant now) {
        Instant expiresAt = now.plus(properties.refreshTokenTtl());
        JwtClaimsSet claims = baseClaims(userId, now, expiresAt, tokenId)
                .claim(JwtClaims.TOKEN_TYPE, JwtClaims.REFRESH_TOKEN_TYPE)
                .build();
        return new IssuedToken(encode(claims), expiresAt);
    }

    @Override
    public RefreshTokenClaims decodeRefreshToken(String token) {
        Jwt jwt = decode(token);
        if (!JwtClaims.REFRESH_TOKEN_TYPE.equals(jwt.getClaimAsString(JwtClaims.TOKEN_TYPE))) {
            log.info("Rejected a non-refresh token at the refresh endpoint");
            throw invalidToken();
        }
        try {
            return new RefreshTokenClaims(UserId.of(jwt.getSubject()), UUID.fromString(jwt.getId()));
        } catch (IllegalArgumentException | NullPointerException malformed) {
            log.info("Refresh token carried an unusable subject or id");
            throw invalidToken();
        }
    }

    private JwtClaimsSet.Builder baseClaims(UserId userId, Instant now, Instant expiresAt, UUID tokenId) {
        return JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(userId.toString())
                .id(tokenId.toString())
                .issuedAt(now)
                .expiresAt(expiresAt);
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(ALGORITHM).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private Jwt decode(String token) {
        try {
            return decoder.decode(token);
        } catch (JwtException rejected) {
            log.debug("Token rejected: {}", rejected.getMessage());
            throw invalidToken();
        }
    }

    private static AuthenticationFailedException invalidToken() {
        return new AuthenticationFailedException(ErrorCode.INVALID_TOKEN,
                "Token is invalid or has expired");
    }

    static List<String> supportedAlgorithms() {
        return List.of(ALGORITHM.getName());
    }
}
