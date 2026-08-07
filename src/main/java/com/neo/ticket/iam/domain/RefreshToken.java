package com.neo.ticket.iam.domain;

import com.neo.ticket.shared.domain.Invariants;
import com.neo.ticket.shared.domain.valueobject.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @Column(name = "token_id", nullable = false, updatable = false)
    private UUID tokenId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    protected RefreshToken() {
    }

    private RefreshToken(UUID tokenId, UserId userId, UUID familyId, Instant issuedAt, Instant expiresAt) {
        this.tokenId = Invariants.requirePresent(tokenId, "refreshToken.tokenId");
        this.userId = Invariants.requirePresent(userId, "refreshToken.userId").value();
        this.familyId = Invariants.requirePresent(familyId, "refreshToken.familyId");
        this.issuedAt = Invariants.requirePresent(issuedAt, "refreshToken.issuedAt");
        this.expiresAt = Invariants.requirePresent(expiresAt, "refreshToken.expiresAt");
        Invariants.require(expiresAt.isAfter(issuedAt), "refresh token must expire after it is issued");
    }

    public static RefreshToken issueNewFamily(UUID tokenId, UserId userId, Instant issuedAt, Instant expiresAt) {
        return new RefreshToken(tokenId, userId, UUID.randomUUID(), issuedAt, expiresAt);
    }

    public RefreshToken rotate(UUID successorTokenId, Instant now, Instant successorExpiresAt) {
        revoke(now);
        this.replacedBy = successorTokenId;
        return new RefreshToken(successorTokenId, userId(), familyId, now, successorExpiresAt);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            this.revokedAt = Invariants.requirePresent(now, "revokedAt");
        }
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isUsableAt(Instant now) {
        return !isRevoked() && !isExpiredAt(now);
    }

    public UUID tokenId() {
        return tokenId;
    }

    public UserId userId() {
        return new UserId(userId);
    }

    public UUID familyId() {
        return familyId;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public UUID replacedBy() {
        return replacedBy;
    }
}
