package com.neo.ticket.reservation.domain;

import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.reservation.domain.valueobject.ReservationId;
import com.neo.ticket.reservation.domain.valueobject.ReservationStatus;
import com.neo.ticket.shared.domain.AggregateRoot;
import com.neo.ticket.shared.domain.Invariants;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.error.BusinessRuleViolationException;
import com.neo.ticket.shared.error.ErrorCode;
import com.neo.ticket.shared.error.ForbiddenOperationException;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class Reservation extends AggregateRoot<ReservationId> {

    public static final int MAX_SEATS_PER_RESERVATION = 10;

    public static final int MIN_SEATS_PER_RESERVATION = 1;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ReservationStatus status;

    @Column(name = "seats", nullable = false, updatable = false)
    private int seats;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Reservation() {
    }

    private Reservation(ReservationId id, EventId eventId, UserId userId, int seats, Instant now) {
        this.id = Invariants.requirePresent(id, "reservation.id").value();
        this.eventId = Invariants.requirePresent(eventId, "reservation.eventId").value();
        this.userId = Invariants.requirePresent(userId, "reservation.userId").value();
        this.seats = Invariants.requireRange(seats, "seats",
                MIN_SEATS_PER_RESERVATION, MAX_SEATS_PER_RESERVATION);
        this.status = ReservationStatus.PENDING;
        this.createdAt = Invariants.requirePresent(now, "reservation.createdAt");
        this.updatedAt = now;
    }

    public static Reservation place(ReservationId id, EventId eventId, UserId userId,
                                    int seats, Instant now) {
        Reservation reservation = new Reservation(id, eventId, userId, seats, now);
        reservation.raise(new ReservationCreated(id, eventId, userId, seats, now));
        return reservation;
    }

    public void confirm(Actor actor, Instant now) {
        assertManageableBy(actor);
        transitionTo(ReservationStatus.CONFIRMED, now);
        raise(new ReservationConfirmed(id(), eventId(), userId(), seats, now));
    }

    public void cancel(Actor actor, Instant now) {
        assertManageableBy(actor);
        transitionTo(ReservationStatus.CANCELLED, now);
        raise(new ReservationCancelled(id(), eventId(), userId(), seats, now));
    }

    public void assertManageableBy(Actor actor) {
        Invariants.requirePresent(actor, "actor");
        if (!actor.mayAdminister(userId())) {
            throw new ForbiddenOperationException(ErrorCode.NOT_RESOURCE_OWNER,
                    "Reservation %s belongs to another customer".formatted(id),
                    Map.of("reservationId", id.toString()));
        }
    }

    public boolean holdsSeats() {
        return status.holdsSeats();
    }

    private void transitionTo(ReservationStatus target, Instant now) {
        if (status == target) {
            throw new BusinessRuleViolationException(ErrorCode.ILLEGAL_STATUS_TRANSITION,
                    "Reservation %s is already %s".formatted(id, target),
                    Map.of("currentStatus", status.name()));
        }
        if (!status.canTransitionTo(target)) {
            throw new BusinessRuleViolationException(ErrorCode.ILLEGAL_STATUS_TRANSITION,
                    "Reservation %s cannot go from %s to %s".formatted(id, status, target),
                    Map.of("currentStatus", status.name(), "requestedStatus", target.name()));
        }
        this.status = target;
        this.updatedAt = Invariants.requirePresent(now, "updatedAt");
    }

    @Override
    public ReservationId id() {
        return new ReservationId(id);
    }

    public EventId eventId() {
        return new EventId(eventId);
    }

    public UserId userId() {
        return new UserId(userId);
    }

    public ReservationStatus status() {
        return status;
    }

    public int seats() {
        return seats;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }
}
