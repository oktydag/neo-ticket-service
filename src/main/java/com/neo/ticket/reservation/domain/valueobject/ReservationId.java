package com.neo.ticket.reservation.domain.valueobject;

import com.neo.ticket.shared.domain.EntityId;
import com.neo.ticket.shared.domain.Invariants;
import java.util.UUID;

public record ReservationId(UUID value) implements EntityId {

    public ReservationId {
        Invariants.requirePresent(value, "reservationId");
    }

    public static ReservationId newId() {
        return new ReservationId(UUID.randomUUID());
    }

    public static ReservationId of(String value) {
        return new ReservationId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
