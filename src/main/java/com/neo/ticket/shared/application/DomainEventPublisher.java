package com.neo.ticket.shared.application;

import com.neo.ticket.shared.domain.AggregateRoot;
import com.neo.ticket.shared.domain.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    public DomainEventPublisher(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    public void publishEventsOf(AggregateRoot<?> aggregate) {
        aggregate.drainDomainEvents().forEach(delegate::publishEvent);
    }

    public void publish(DomainEvent event) {
        delegate.publishEvent(event);
    }
}
