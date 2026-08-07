package com.neo.ticket.iam.application;

import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.domain.valueobject.UserId;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface TokenIssuer {

    IssuedToken issueAccessToken(UserId userId, Set<Role> roles, Instant now);

    IssuedToken issueRefreshToken(UserId userId, UUID tokenId, Instant now);

    RefreshTokenClaims decodeRefreshToken(String token);

    record IssuedToken(String value, Instant expiresAt) {
    }

    record RefreshTokenClaims(UserId userId, UUID tokenId) {
    }
}
