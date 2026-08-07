package com.neo.ticket.iam.domain;

import com.neo.ticket.iam.domain.valueobject.Email;
import com.neo.ticket.shared.domain.AuditableEvent;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.domain.valueobject.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public record UserRegistered(UserId userId, Email email, Set<Role> roles, Instant occurredAt)
        implements AuditableEvent {

    @Override
    public String action() {
        return "USER_REGISTERED";
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
