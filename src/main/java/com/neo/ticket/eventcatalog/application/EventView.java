package com.neo.ticket.eventcatalog.application;

import com.neo.ticket.eventcatalog.domain.Event;
import java.time.Instant;
import java.util.UUID;

public record EventView(
        UUID id,
        UUID ownerId,
        String title,
        String venue,
        Instant startsAt,
        Instant endsAt,
        int capacity,
        int reservedSeats,
        int remainingSeats,
        boolean published,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public static EventView from(Event event) {
        return new EventView(
                event.id().value(),
                event.ownerId().value(),
                event.title(),
                event.venue(),
                event.schedule().startsAt(),
                event.schedule().endsAt(),
                event.capacity().total(),
                event.capacity().reserved(),
                event.capacity().remaining(),
                event.isPublished(),
                event.publishedAt(),
                event.createdAt(),
                event.updatedAt(),
                event.version());
    }
}
