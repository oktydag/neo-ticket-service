package com.neo.ticket.reservation.application.command.handlers;

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

@Service
public class ConfirmReservationHandler {

    private final ReservationRepository reservationRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    public ConfirmReservationHandler(ReservationRepository reservationRepository,
                                     DomainEventPublisher domainEventPublisher,
                                     Clock clock) {
        this.reservationRepository = reservationRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public ReservationView handle(Actor actor, ReservationId reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(() ->
                ResourceNotFoundException.of(
                        ErrorCode.RESERVATION_NOT_FOUND, ReservationResource.TYPE, reservationId));
        reservation.confirm(actor, clock.instant());
        Reservation saved = reservationRepository.save(reservation);

        domainEventPublisher.publishEventsOf(reservation);
        return ReservationView.from(saved);
    }
}
