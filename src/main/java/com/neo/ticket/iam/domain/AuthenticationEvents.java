package com.neo.ticket.iam.domain;

import com.neo.ticket.shared.domain.AuditableEvent;
import com.neo.ticket.shared.domain.valueobject.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class AuthenticationEvents {

    private AuthenticationEvents() {
    }

    public record UserLoggedIn(UserId userId, Instant occurredAt) implements AuditableEvent {

        @Override
        public String action() {
            return "USER_LOGGED_IN";
        }

        @Override
        public String resourceType() {
            return UserResource.TYPE;
        }

        @Override
        public String resourceId() {
            return userId.toString();
        }

        @Override
        public Optional<UserId> actorId() {
            return Optional.of(userId);
        }
    }

    public record LoginFailed(String attemptedEmail, Instant occurredAt) implements AuditableEvent {

        @Override
        public String action() {
            return "LOGIN_FAILED";
        }

        @Override
        public String resourceType() {
            return UserResource.AUTHENTICATION_TYPE;
        }

        @Override
        public String resourceId() {
            return attemptedEmail;
        }
    }

    public record RefreshTokenReuseDetected(UserId userId, UUID familyId, int revokedCount,
                                            Instant occurredAt) implements AuditableEvent {

        @Override
        public String action() {
            return "REFRESH_TOKEN_REUSE_DETECTED";
        }

        @Override
        public String resourceType() {
            return UserResource.TYPE;
        }

        @Override
        public String resourceId() {
            return userId.toString();
        }

        @Override
        public Optional<UserId> actorId() {
            return Optional.of(userId);
        }
    }
}
