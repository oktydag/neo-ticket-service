package com.neo.ticket.eventcatalog.application.query.handlers;

import com.neo.ticket.eventcatalog.application.EventView;
import com.neo.ticket.eventcatalog.domain.Event;
import com.neo.ticket.eventcatalog.domain.EventRepository;
import com.neo.ticket.eventcatalog.domain.EventResource;
import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.error.ErrorCode;
import com.neo.ticket.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetEventHandler {

    private final EventRepository eventRepository;

    public GetEventHandler(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public EventView handle(Actor actor, EventId eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> notFound(eventId));

        if (!event.isVisibleTo(actor)) {
            throw notFound(eventId);
        }
        return EventView.from(event);
    }

    private static ResourceNotFoundException notFound(EventId eventId) {
        return ResourceNotFoundException.of(ErrorCode.EVENT_NOT_FOUND, EventResource.TYPE, eventId);
    }
}
