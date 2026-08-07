package com.neo.ticket.testsupport;

import com.neo.ticket.shared.application.DomainEventPublisher;
import com.neo.ticket.shared.domain.DomainEvent;
import java.util.ArrayList;
import java.util.List;

public final class RecordingEventPublisher extends DomainEventPublisher {

    private final List<Object> published;

    private RecordingEventPublisher(List<Object> sink) {
        super(sink::add);
        this.published = sink;
    }

    public static RecordingEventPublisher create() {
        return new RecordingEventPublisher(new ArrayList<>());
    }

    public List<Object> published() {
        return List.copyOf(published);
    }

    public <T extends DomainEvent> List<T> eventsOfType(Class<T> type) {
        return published.stream().filter(type::isInstance).map(type::cast).toList();
    }

    public boolean hasEventOfType(Class<? extends DomainEvent> type) {
        return published.stream().anyMatch(type::isInstance);
    }

    public void clear() {
        published.clear();
    }
}
