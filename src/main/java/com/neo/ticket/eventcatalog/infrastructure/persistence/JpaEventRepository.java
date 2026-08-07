package com.neo.ticket.eventcatalog.infrastructure.persistence;

import com.neo.ticket.eventcatalog.domain.Event;
import com.neo.ticket.eventcatalog.domain.EventRepository;
import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JpaEventRepository implements EventRepository {

    private final EventJpaRepository jpaRepository;

    JpaEventRepository(EventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Event> findById(EventId id) {
        return jpaRepository.findById(id.value());
    }

    @Override
    public Optional<Event> findByIdForUpdate(EventId id) {
        return jpaRepository.findByIdForUpdate(id.value());
    }

    @Override
    public Event save(Event event) {
        return jpaRepository.save(event);
    }
}
