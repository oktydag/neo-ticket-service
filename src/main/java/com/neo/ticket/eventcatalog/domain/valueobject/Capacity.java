package com.neo.ticket.eventcatalog.domain.valueobject;

import com.neo.ticket.shared.domain.Invariants;
import com.neo.ticket.shared.error.BusinessRuleViolationException;
import com.neo.ticket.shared.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Map;
import java.util.Objects;

@Embeddable
public class Capacity {

    public static final int MIN_TOTAL = 1;

    public static final int MAX_TOTAL = 100_000;

    @Column(name = "capacity", nullable = false)
    private int total;

    @Column(name = "reserved_seats", nullable = false)
    private int reserved;

    protected Capacity() {
    }

    private Capacity(int total, int reserved) {
        Invariants.requireRange(total, "capacity", MIN_TOTAL, MAX_TOTAL);
        Invariants.require(reserved >= 0, "reserved seats must not be negative");
        Invariants.require(reserved <= total, "reserved seats must not exceed capacity");
        this.total = total;
        this.reserved = reserved;
    }

    public static Capacity of(int total) {
        return new Capacity(total, 0);
    }

    public static Capacity of(int total, int reserved) {
        return new Capacity(total, reserved);
    }

    public int remaining() {
        return total - reserved;
    }

    public boolean isSoldOut() {
        return remaining() == 0;
    }

    public boolean canAccommodate(int seats) {
        return seats > 0 && seats <= remaining();
    }

    public Capacity reserve(int seats) {
        requirePositive(seats);
        if (seats > remaining()) {
            throw new BusinessRuleViolationException(
                    ErrorCode.INSUFFICIENT_CAPACITY,
                    "Only %d of the requested %d seat(s) are still available".formatted(remaining(), seats),
                    Map.of("requestedSeats", seats, "remainingSeats", remaining()));
        }
        return new Capacity(total, reserved + seats);
    }

    public Capacity release(int seats) {
        requirePositive(seats);
        Invariants.require(seats <= reserved,
                "cannot release %d seat(s) when only %d are reserved".formatted(seats, reserved));
        return new Capacity(total, reserved - seats);
    }

    public Capacity resizeTo(int newTotal) {
        if (newTotal < reserved) {
            throw new BusinessRuleViolationException(
                    ErrorCode.CAPACITY_BELOW_RESERVED,
                    "Capacity cannot be reduced to %d because %d seat(s) are already reserved"
                            .formatted(newTotal, reserved),
                    Map.of("requestedCapacity", newTotal, "reservedSeats", reserved));
        }
        return new Capacity(newTotal, reserved);
    }

    private static void requirePositive(int seats) {
        Invariants.require(seats > 0, "seat count must be positive");
    }

    public int total() {
        return total;
    }

    public int reserved() {
        return reserved;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Capacity that && total == that.total && reserved == that.reserved;
    }

    @Override
    public int hashCode() {
        return Objects.hash(total, reserved);
    }

    @Override
    public String toString() {
        return "%d/%d reserved".formatted(reserved, total);
    }
}
