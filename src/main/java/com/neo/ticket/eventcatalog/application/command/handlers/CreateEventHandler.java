package com.neo.ticket.eventcatalog.application.command.handlers;

import com.neo.ticket.eventcatalog.application.EventView;
import com.neo.ticket.eventcatalog.application.command.CreateEventCommand;
import com.neo.ticket.eventcatalog.domain.Event;
import com.neo.ticket.eventcatalog.domain.EventRepository;
import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.eventcatalog.domain.valueobject.EventSchedule;
import com.neo.ticket.shared.application.DomainEventPublisher;
import com.neo.ticket.shared.domain.valueobject.Actor;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateEventHandler {

    private final EventRepository eventRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    public CreateEventHandler(EventRepository eventRepository,
                              DomainEventPublisher domainEventPublisher,
                              Clock clock) {
        this.eventRepository = eventRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public EventView handle(Actor organizer, CreateEventCommand command) {
        Event event = Event.createDraft(
                EventId.newId(),
                organizer.userId(),
                command.title(),
                command.venue(),
                EventSchedule.of(command.startsAt(), command.endsAt()),
                command.capacity(),
                clock.instant());

        Event saved = eventRepository.save(event);
        domainEventPublisher.publishEventsOf(event);
        return EventView.from(saved);
    }
}
