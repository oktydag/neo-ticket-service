package com.neo.ticket.reservation.application.command.handlers;

import com.neo.ticket.eventcatalog.domain.Event;
import com.neo.ticket.eventcatalog.domain.EventRepository;
import com.neo.ticket.eventcatalog.domain.EventResource;
import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.reservation.application.ReservationView;
import com.neo.ticket.reservation.domain.Reservation;
import com.neo.ticket.reservation.domain.ReservationRepository;
import com.neo.ticket.reservation.domain.valueobject.ReservationId;
import com.neo.ticket.shared.application.DomainEventPublisher;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.error.ErrorCode;
import com.neo.ticket.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class ReserveSeatsHandler {

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    public ReserveSeatsHandler(ReservationRepository reservationRepository,
                               EventRepository eventRepository,
                               DomainEventPublisher domainEventPublisher,
                               Clock clock) {
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public ReservationView handle(Actor customer, EventId eventId, int seats) {
        Instant now = clock.instant();

        // Lock the event row before reading capacity, so the seat count cannot change
        // between the check and the write. This is what prevents overselling.
        Event event = eventRepository.findByIdForUpdate(eventId).orElseThrow(() ->
                ResourceNotFoundException.of(ErrorCode.EVENT_NOT_FOUND, EventResource.TYPE, eventId));
        event.reserveSeats(seats, now);
        eventRepository.save(event);

        Reservation reservation = Reservation.place(
                ReservationId.newId(), eventId, customer.userId(), seats, now);
        Reservation saved = reservationRepository.save(reservation);

        domainEventPublisher.publishEventsOf(reservation);
        return ReservationView.from(saved);
    }
}
