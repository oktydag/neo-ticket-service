package com.neo.ticket.reservation.application.query.handlers;

import com.neo.ticket.reservation.application.ReservationView;
import com.neo.ticket.reservation.domain.Reservation;
import com.neo.ticket.reservation.domain.ReservationRepository;
import com.neo.ticket.reservation.domain.ReservationResource;
import com.neo.ticket.reservation.domain.valueobject.ReservationId;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.error.ErrorCode;
import com.neo.ticket.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetReservationHandler {

    private final ReservationRepository reservationRepository;

    public GetReservationHandler(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public ReservationView handle(Actor actor, ReservationId reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(() ->
                ResourceNotFoundException.of(
                        ErrorCode.RESERVATION_NOT_FOUND, ReservationResource.TYPE, reservationId));
        reservation.assertManageableBy(actor);
        return ReservationView.from(reservation);
    }
}
