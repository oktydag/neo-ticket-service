package com.neo.ticket.eventcatalog.application.command.handlers;

import com.neo.ticket.eventcatalog.application.EventView;
import com.neo.ticket.eventcatalog.domain.Event;
import com.neo.ticket.eventcatalog.domain.EventRepository;
import com.neo.ticket.eventcatalog.domain.EventResource;
import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.shared.application.DomainEventPublisher;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.error.ErrorCode;
import com.neo.ticket.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class PublishEventHandler {

    private final EventRepository eventRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    public PublishEventHandler(EventRepository eventRepository,
                               DomainEventPublisher domainEventPublisher,
                               Clock clock) {
        this.eventRepository = eventRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public EventView handle(Actor actor, EventId eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                ResourceNotFoundException.of(ErrorCode.EVENT_NOT_FOUND, EventResource.TYPE, eventId));
        event.publish(actor, clock.instant());

        Event saved = eventRepository.save(event);
        domainEventPublisher.publishEventsOf(event);
        return EventView.from(saved);
    }
}
