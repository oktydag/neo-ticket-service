package com.neo.ticket.reservation.application.command.handlers;

import com.neo.ticket.eventcatalog.domain.Event;
import com.neo.ticket.eventcatalog.domain.EventRepository;
import com.neo.ticket.eventcatalog.domain.EventResource;
import com.neo.ticket.reservation.application.ReservationView;
import com.neo.ticket.reservation.domain.Reservation;
import com.neo.ticket.reservation.domain.ReservationRepository;
import com.neo.ticket.reservation.domain.ReservationResource;
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
public class CancelReservationHandler {

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    public CancelReservationHandler(ReservationRepository reservationRepository,
                                    EventRepository eventRepository,
                                    DomainEventPublisher domainEventPublisher,
                                    Clock clock) {
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public ReservationView handle(Actor actor, ReservationId reservationId) {
        Instant now = clock.instant();
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(() ->
                ResourceNotFoundException.of(
                        ErrorCode.RESERVATION_NOT_FOUND, ReservationResource.TYPE, reservationId));
        int seatsToRelease = reservation.seats();

        reservation.cancel(actor, now);

        Event event = eventRepository.findByIdForUpdate(reservation.eventId()).orElseThrow(() ->
                ResourceNotFoundException.of(
                        ErrorCode.EVENT_NOT_FOUND, EventResource.TYPE, reservation.eventId()));
        event.releaseSeats(seatsToRelease, now);
        eventRepository.save(event);

        Reservation saved = reservationRepository.save(reservation);
        domainEventPublisher.publishEventsOf(reservation);
        return ReservationView.from(saved);
    }
}
