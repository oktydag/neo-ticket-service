package com.neo.ticket.eventcatalog.infrastructure.persistence;

import com.neo.ticket.eventcatalog.application.EventCatalogQueries;
import com.neo.ticket.eventcatalog.application.EventView;
import com.neo.ticket.eventcatalog.application.query.EventSearchCriteria;
import com.neo.ticket.eventcatalog.domain.Event;
import com.neo.ticket.shared.domain.valueobject.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Repository
class JpaEventCatalogQueries implements EventCatalogQueries {

    private final EventJpaRepository jpaRepository;

    JpaEventCatalogQueries(EventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventView> findPublished(EventSearchCriteria criteria, Pageable pageable) {
        return search(List.of(EventSpecifications.isPublished()), criteria, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventView> findOwnedBy(UserId ownerId, EventSearchCriteria criteria, Pageable pageable) {
        return search(List.of(EventSpecifications.ownedBy(ownerId)), criteria, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventView> findAll(EventSearchCriteria criteria, Pageable pageable) {
        return search(List.of(), criteria, pageable);
    }

    private Page<EventView> search(List<Specification<Event>> mandatory,
                                   EventSearchCriteria criteria, Pageable pageable) {
        List<Specification<Event>> specifications = new ArrayList<>(mandatory);
        criteria.fromInstant().map(EventSpecifications::startsAtOrAfter).ifPresent(specifications::add);
        criteria.toInstant().map(EventSpecifications::startsAtOrBefore).ifPresent(specifications::add);
        criteria.searchText().map(EventSpecifications::matchesText).ifPresent(specifications::add);

        Specification<Event> combined = specifications.isEmpty()
                ? Specification.unrestricted()
                : Specification.allOf(specifications);
        return jpaRepository.findAll(combined, pageable).map(EventView::from);
    }
}
