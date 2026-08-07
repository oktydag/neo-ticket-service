package com.neo.ticket.eventcatalog.domain.valueobject;

import com.neo.ticket.shared.domain.EntityId;
import com.neo.ticket.shared.domain.Invariants;
import java.util.UUID;

public record EventId(UUID value) implements EntityId {

    public EventId {
        Invariants.requirePresent(value, "eventId");
    }

    public static EventId newId() {
        return new EventId(UUID.randomUUID());
    }

    public static EventId of(String value) {
        return new EventId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
