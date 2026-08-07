package com.neo.ticket.eventcatalog.domain.valueobject;

import com.neo.ticket.shared.domain.Invariants;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Embeddable
public class EventSchedule {

    public static final Duration MIN_DURATION = Duration.ofMinutes(1);
    public static final Duration MAX_DURATION = Duration.ofDays(30);

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    protected EventSchedule() {
    }

    private EventSchedule(Instant startsAt, Instant endsAt) {
        this.startsAt = Invariants.requirePresent(startsAt, "startsAt");
        this.endsAt = Invariants.requirePresent(endsAt, "endsAt");
        Invariants.require(endsAt.isAfter(startsAt), "endsAt must be after startsAt");

        Duration duration = Duration.between(startsAt, endsAt);
        Invariants.require(duration.compareTo(MIN_DURATION) >= 0,
                "event must run for at least %d minute(s)".formatted(MIN_DURATION.toMinutes()));
        Invariants.require(duration.compareTo(MAX_DURATION) <= 0,
                "event must not run for longer than %d days".formatted(MAX_DURATION.toDays()));
    }

    public static EventSchedule of(Instant startsAt, Instant endsAt) {
        return new EventSchedule(startsAt, endsAt);
    }

    public boolean startsAfter(Instant moment) {
        return startsAt.isAfter(moment);
    }

    public boolean hasStartedBy(Instant moment) {
        return !startsAt.isAfter(moment);
    }

    public Duration duration() {
        return Duration.between(startsAt, endsAt);
    }

    public Instant startsAt() {
        return startsAt;
    }

    public Instant endsAt() {
        return endsAt;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EventSchedule that
                && Objects.equals(startsAt, that.startsAt)
                && Objects.equals(endsAt, that.endsAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startsAt, endsAt);
    }

    @Override
    public String toString() {
        return "%s..%s".formatted(startsAt, endsAt);
    }
}
