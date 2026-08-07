package com.neo.ticket.eventcatalog.domain;

import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.shared.domain.AuditableEvent;
import com.neo.ticket.shared.domain.valueobject.UserId;
import java.time.Instant;

public record EventCreated(EventId eventId, UserId ownerId, Instant occurredAt) implements AuditableEvent {

    @Override
    public String action() {
        return "EVENT_CREATED";
    }

    @Override
    public String resourceType() {
        return EventResource.TYPE;
    }

    @Override
    public String resourceId() {
        return eventId.toString();
    }
}
