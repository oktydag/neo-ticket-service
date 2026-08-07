package com.neo.ticket.reservation.application;

import com.neo.ticket.reservation.domain.Reservation;
import com.neo.ticket.reservation.domain.valueobject.ReservationStatus;
import java.time.Instant;
import java.util.UUID;

public record ReservationView(
        UUID id,
        UUID eventId,
        UUID userId,
        ReservationStatus status,
        int seats,
        Instant createdAt,
        Instant updatedAt) {

    public static ReservationView from(Reservation reservation) {
        return new ReservationView(
                reservation.id().value(),
                reservation.eventId().value(),
                reservation.userId().value(),
                reservation.status(),
                reservation.seats(),
                reservation.createdAt(),
                reservation.updatedAt());
    }
}
