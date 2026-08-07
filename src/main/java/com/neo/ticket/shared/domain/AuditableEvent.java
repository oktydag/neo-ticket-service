package com.neo.ticket.shared.domain;

import com.neo.ticket.shared.domain.valueobject.UserId;
import java.util.Optional;

public interface AuditableEvent extends DomainEvent {

    String action();

    String resourceType();

    String resourceId();

    default Optional<UserId> actorId() {
        return Optional.empty();
    }
}
