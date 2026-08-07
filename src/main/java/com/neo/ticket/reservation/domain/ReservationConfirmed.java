package com.neo.ticket.reservation.domain;

import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.reservation.domain.valueobject.ReservationId;
import com.neo.ticket.shared.domain.AuditableEvent;
import com.neo.ticket.shared.domain.valueobject.UserId;
import java.time.Instant;

public record ReservationConfirmed(ReservationId reservationId, EventId eventId, UserId userId,
                                   int seats, Instant occurredAt) implements AuditableEvent {

    @Override
    public String action() {
        return "RESERVATION_CONFIRMED";
    }

    @Override
    public String resourceType() {
        return ReservationResource.TYPE;
    }

    @Override
    public String resourceId() {
        return reservationId.toString();
    }
}
