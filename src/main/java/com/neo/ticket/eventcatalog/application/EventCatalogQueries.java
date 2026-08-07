package com.neo.ticket.eventcatalog.application;

import com.neo.ticket.eventcatalog.application.query.EventSearchCriteria;
import com.neo.ticket.shared.domain.valueobject.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventCatalogQueries {

    Page<EventView> findPublished(EventSearchCriteria criteria, Pageable pageable);

    Page<EventView> findOwnedBy(UserId ownerId, EventSearchCriteria criteria, Pageable pageable);

    Page<EventView> findAll(EventSearchCriteria criteria, Pageable pageable);
}
