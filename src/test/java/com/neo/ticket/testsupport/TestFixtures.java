package com.neo.ticket.testsupport;

import com.neo.ticket.eventcatalog.domain.Event;
import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.eventcatalog.domain.valueobject.EventSchedule;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.domain.valueobject.UserId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

public final class TestFixtures {

    public static final Instant NOW = Instant.parse("2026-06-01T10:00:00Z");

    private TestFixtures() {
    }

    public static Actor organizer(UserId userId) {
        return new Actor(userId, Set.of(Role.ORGANIZER));
    }

    public static Actor admin() {
        return new Actor(UserId.newId(), Set.of(Role.ADMIN));
    }

    public static EventSchedule futureSchedule() {
        return EventSchedule.of(NOW.plus(30, ChronoUnit.DAYS), NOW.plus(30, ChronoUnit.DAYS).plusSeconds(7200));
    }

    public static Event draftEvent(UserId ownerId, int capacity) {
        return Event.createDraft(EventId.newId(), ownerId, "Concurrency Conference",
                "Neo Arena, Hall B", futureSchedule(), capacity, NOW);
    }

    public static Event publishedEvent(UserId ownerId, int capacity) {
        Event event = draftEvent(ownerId, capacity);
        event.publish(organizer(ownerId), NOW);
        event.drainDomainEvents();
        return event;
    }

}
