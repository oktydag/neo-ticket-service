package com.neo.ticket.reservation.infrastructure.persistence;

import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.reservation.domain.Reservation;
import com.neo.ticket.reservation.domain.ReservationRepository;
import com.neo.ticket.reservation.domain.valueobject.ReservationId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JpaReservationRepository implements ReservationRepository {

    private final ReservationJpaRepository jpaRepository;

    JpaReservationRepository(ReservationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Reservation> findById(ReservationId id) {
        return jpaRepository.findById(id.value());
    }

    @Override
    public Reservation save(Reservation reservation) {
        return jpaRepository.save(reservation);
    }

    @Override
    public int totalSeatsHeldFor(EventId eventId) {
        return jpaRepository.sumSeatsHeldFor(eventId.value());
    }
}
