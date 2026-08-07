package com.neo.ticket.testsupport;

import com.neo.ticket.iam.application.TokenIssuer;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.error.AuthenticationFailedException;
import com.neo.ticket.shared.error.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class FakeTokenIssuer implements TokenIssuer {

    public static final Duration ACCESS_TTL = Duration.ofMinutes(15);
    public static final Duration REFRESH_TTL = Duration.ofDays(7);

    private static final String ACCESS_PREFIX = "access|";
    private static final String REFRESH_PREFIX = "refresh|";

    @Override
    public IssuedToken issueAccessToken(UserId userId, Set<Role> roles, Instant now) {
        String value = ACCESS_PREFIX + userId + '|' + UUID.randomUUID();
        return new IssuedToken(value, now.plus(ACCESS_TTL));
    }

    @Override
    public IssuedToken issueRefreshToken(UserId userId, UUID tokenId, Instant now) {
        String value = REFRESH_PREFIX + userId + '|' + tokenId;
        return new IssuedToken(value, now.plus(REFRESH_TTL));
    }

    @Override
    public RefreshTokenClaims decodeRefreshToken(String token) {
        if (token == null || !token.startsWith(REFRESH_PREFIX)) {
            throw new AuthenticationFailedException(ErrorCode.INVALID_TOKEN,
                    "Token is invalid or has expired");
        }
        String[] parts = token.split("\\|");
        return new RefreshTokenClaims(UserId.of(parts[1]), UUID.fromString(parts[2]));
    }
}
