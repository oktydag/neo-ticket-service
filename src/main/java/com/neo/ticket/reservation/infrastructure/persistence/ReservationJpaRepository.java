package com.neo.ticket.reservation.infrastructure.persistence;

import com.neo.ticket.reservation.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface ReservationJpaRepository extends JpaRepository<Reservation, UUID> {

    @Query("""
            select coalesce(sum(r.seats), 0)
              from Reservation r
             where r.eventId = :eventId
               and r.status <> com.neo.ticket.reservation.domain.valueobject.ReservationStatus.CANCELLED
            """)
    int sumSeatsHeldFor(@Param("eventId") UUID eventId);
}
