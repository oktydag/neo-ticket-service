package com.neo.ticket.idempotency.domain;

import com.neo.ticket.idempotency.domain.valueobject.IdempotencyStatus;
import com.neo.ticket.shared.domain.Hashing;
import com.neo.ticket.shared.domain.Invariants;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.web.IdempotencyHeaders;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyRecord {

    public static final Duration DEFAULT_TTL = Duration.ofHours(24);

    public static final int MAX_RESPONSE_BODY_LENGTH = 8_192;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, updatable = false,
            length = IdempotencyHeaders.MAX_KEY_LENGTH)
    private String idempotencyKey;

    @Column(name = "endpoint", nullable = false, updatable = false, length = 200)
    private String endpoint;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "request_hash", nullable = false, updatable = false,
            length = Hashing.SHA256_HEX_LENGTH)
    private String requestHash;

    @Column(name = "response_hash", length = Hashing.SHA256_HEX_LENGTH)
    private String responseHash;

    @Column(name = "response_body", length = MAX_RESPONSE_BODY_LENGTH)
    private String responseBody;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private IdempotencyStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected IdempotencyRecord() {
    }

    private IdempotencyRecord(String idempotencyKey, String endpoint, UserId userId,
                              String requestHash, Instant now, Duration ttl) {
        this.id = UUID.randomUUID();
        this.idempotencyKey = Invariants.requireText(idempotencyKey, "idempotencyKey",
                IdempotencyHeaders.MIN_KEY_LENGTH, IdempotencyHeaders.MAX_KEY_LENGTH);
        this.endpoint = Invariants.requireText(endpoint, "endpoint", 1, 200);
        this.userId = Invariants.requirePresent(userId, "idempotency.userId").value();
        this.requestHash = Invariants.requirePresent(requestHash, "requestHash");
        this.status = IdempotencyStatus.IN_PROGRESS;
        this.createdAt = Invariants.requirePresent(now, "createdAt");
        this.expiresAt = now.plus(Invariants.requirePresent(ttl, "ttl"));
    }

    public static IdempotencyRecord claim(String idempotencyKey, String endpoint, UserId userId,
                                          String requestHash, Instant now, Duration ttl) {
        return new IdempotencyRecord(idempotencyKey, endpoint, userId, requestHash, now, ttl);
    }

    public void complete(String serialisedResponse, int httpStatus) {
        Invariants.requirePresent(serialisedResponse, "response");
        this.responseHash = Hashing.sha256Hex(serialisedResponse);
        this.responseBody = truncateForStorage(serialisedResponse);
        this.responseStatus = httpStatus;
        this.status = IdempotencyStatus.COMPLETED;
    }

    public void fail() {
        this.status = IdempotencyStatus.FAILED;
    }

    public void restart(String requestHash, Instant now, Duration ttl) {
        this.requestHash = Invariants.requirePresent(requestHash, "requestHash");
        this.status = IdempotencyStatus.IN_PROGRESS;
        this.responseBody = null;
        this.responseHash = null;
        this.responseStatus = null;
        this.expiresAt = now.plus(ttl);
    }

    public boolean matchesRequest(String candidateRequestHash) {
        return requestHash.equals(candidateRequestHash);
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isReplayable() {
        return status == IdempotencyStatus.COMPLETED && responseBody != null;
    }

    private static String truncateForStorage(String response) {
        return response.length() <= MAX_RESPONSE_BODY_LENGTH ? response : null;
    }

    public UUID id() {
        return id;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public String endpoint() {
        return endpoint;
    }

    public UserId userId() {
        return new UserId(userId);
    }

    public String requestHash() {
        return requestHash;
    }

    public String responseHash() {
        return responseHash;
    }

    public String responseBody() {
        return responseBody;
    }

    public Integer responseStatus() {
        return responseStatus;
    }

    public IdempotencyStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }
}
