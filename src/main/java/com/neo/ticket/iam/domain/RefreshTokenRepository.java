package com.neo.ticket.iam.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByTokenId(UUID tokenId);

    RefreshToken save(RefreshToken refreshToken);

    int revokeFamily(UUID familyId, Instant now);

    int deleteExpiredBefore(Instant cutoff);
}
