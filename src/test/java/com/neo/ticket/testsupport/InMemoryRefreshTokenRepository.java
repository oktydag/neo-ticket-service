package com.neo.ticket.testsupport;

import com.neo.ticket.iam.domain.RefreshToken;
import com.neo.ticket.iam.domain.RefreshTokenRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

    private final Map<UUID, RefreshToken> byTokenId = new LinkedHashMap<>();

    @Override
    public Optional<RefreshToken> findByTokenId(UUID tokenId) {
        return Optional.ofNullable(byTokenId.get(tokenId));
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        byTokenId.put(refreshToken.tokenId(), refreshToken);
        return refreshToken;
    }

    @Override
    public int revokeFamily(UUID familyId, Instant now) {
        int revoked = 0;
        for (RefreshToken token : byTokenId.values()) {
            if (token.familyId().equals(familyId) && !token.isRevoked()) {
                token.revoke(now);
                revoked++;
            }
        }
        return revoked;
    }

    @Override
    public int deleteExpiredBefore(Instant cutoff) {
        int before = byTokenId.size();
        byTokenId.values().removeIf(token -> token.expiresAt().isBefore(cutoff));
        return before - byTokenId.size();
    }

    public int count() {
        return byTokenId.size();
    }
}
