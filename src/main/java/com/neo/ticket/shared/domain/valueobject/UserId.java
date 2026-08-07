package com.neo.ticket.shared.domain.valueobject;

import com.neo.ticket.shared.domain.EntityId;
import com.neo.ticket.shared.domain.Invariants;
import java.util.UUID;

public record UserId(UUID value) implements EntityId {

    public UserId {
        Invariants.requirePresent(value, "userId");
    }

    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(String value) {
        return new UserId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
