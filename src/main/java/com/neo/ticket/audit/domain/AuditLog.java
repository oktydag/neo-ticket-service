package com.neo.ticket.audit.domain;

import com.neo.ticket.shared.domain.Invariants;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.web.RequestMetadata;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    public static final int MAX_ACTION_LENGTH = 64;
    public static final int MAX_RESOURCE_TYPE_LENGTH = 64;
    public static final int MAX_RESOURCE_ID_LENGTH = 64;

    public static final int MAX_IP_LENGTH = 45;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "action", nullable = false, updatable = false, length = MAX_ACTION_LENGTH)
    private String action;

    @Column(name = "resource_type", nullable = false, updatable = false, length = MAX_RESOURCE_TYPE_LENGTH)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, updatable = false, length = MAX_RESOURCE_ID_LENGTH)
    private String resourceId;

    @Column(name = "ip", nullable = false, updatable = false, length = MAX_IP_LENGTH)
    private String ip;

    @Column(name = "user_agent", nullable = false, updatable = false,
            length = RequestMetadata.MAX_USER_AGENT_LENGTH)
    private String userAgent;

    @Column(name = "request_id", nullable = false, updatable = false, length = 64)
    private String requestId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    private AuditLog(UserId actorId, String action, String resourceType, String resourceId,
                     RequestMetadata metadata, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.actorId = actorId != null ? actorId.value() : null;
        this.action = Invariants.requireText(action, "action", 1, MAX_ACTION_LENGTH);
        this.resourceType = Invariants.requireText(resourceType, "resourceType", 1, MAX_RESOURCE_TYPE_LENGTH);
        this.resourceId = Invariants.requireText(resourceId, "resourceId", 1, MAX_RESOURCE_ID_LENGTH);
        this.ip = truncate(metadata.clientIp(), MAX_IP_LENGTH);
        this.userAgent = truncate(metadata.userAgent(), RequestMetadata.MAX_USER_AGENT_LENGTH);
        this.requestId = truncate(metadata.requestId(), 64);
        this.createdAt = Invariants.requirePresent(createdAt, "createdAt");
    }

    public static AuditLog record(UserId actorId, String action, String resourceType,
                                  String resourceId, RequestMetadata metadata, Instant createdAt) {
        return new AuditLog(actorId, action, resourceType, resourceId, metadata, createdAt);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return RequestMetadata.UNKNOWN;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public UUID id() {
        return id;
    }

    public UserId actorId() {
        return actorId != null ? new UserId(actorId) : null;
    }

    public String action() {
        return action;
    }

    public String resourceType() {
        return resourceType;
    }

    public String resourceId() {
        return resourceId;
    }

    public String ip() {
        return ip;
    }

    public String userAgent() {
        return userAgent;
    }

    public String requestId() {
        return requestId;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
