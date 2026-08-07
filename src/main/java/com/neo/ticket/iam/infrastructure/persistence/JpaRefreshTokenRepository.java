package com.neo.ticket.iam.infrastructure.persistence;

import com.neo.ticket.iam.domain.RefreshToken;
import com.neo.ticket.iam.domain.RefreshTokenRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaRefreshTokenRepository implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    JpaRefreshTokenRepository(RefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<RefreshToken> findByTokenId(UUID tokenId) {
        return jpaRepository.findById(tokenId);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return jpaRepository.save(refreshToken);
    }

    @Override
    public int revokeFamily(UUID familyId, Instant now) {
        return jpaRepository.revokeFamily(familyId, now);
    }

    @Override
    public int deleteExpiredBefore(Instant cutoff) {
        return jpaRepository.deleteExpiredBefore(cutoff);
    }
}
