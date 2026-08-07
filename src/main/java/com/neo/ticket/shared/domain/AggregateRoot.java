package com.neo.ticket.shared.domain;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@MappedSuperclass
public abstract class AggregateRoot<I extends EntityId> {

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public abstract I id();

    protected void raise(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public List<DomainEvent> drainDomainEvents() {
        List<DomainEvent> drained = List.copyOf(domainEvents);
        domainEvents.clear();
        return drained;
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AggregateRoot<?> that) || !getClass().equals(other.getClass())) {
            return false;
        }
        return id() != null && id().equals(that.id());
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }
}
