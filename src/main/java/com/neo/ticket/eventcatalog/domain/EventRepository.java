package com.neo.ticket.eventcatalog.domain;

import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import java.util.Optional;

public interface EventRepository {

    Optional<Event> findById(EventId id);

    Optional<Event> findByIdForUpdate(EventId id);

    Event save(Event event);
}
