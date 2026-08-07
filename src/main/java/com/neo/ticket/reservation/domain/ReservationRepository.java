package com.neo.ticket.reservation.domain;

import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.reservation.domain.valueobject.ReservationId;
import java.util.Optional;

public interface ReservationRepository {

    Optional<Reservation> findById(ReservationId id);

    Reservation save(Reservation reservation);

    int totalSeatsHeldFor(EventId eventId);
}
