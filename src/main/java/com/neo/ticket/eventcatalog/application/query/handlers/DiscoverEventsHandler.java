package com.neo.ticket.eventcatalog.application.query.handlers;

import com.neo.ticket.eventcatalog.application.EventCatalogQueries;
import com.neo.ticket.eventcatalog.application.EventView;
import com.neo.ticket.eventcatalog.application.query.EventSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DiscoverEventsHandler {

    private final EventCatalogQueries queries;

    public DiscoverEventsHandler(EventCatalogQueries queries) {
        this.queries = queries;
    }

    public Page<EventView> handle(EventSearchCriteria criteria, Pageable pageable) {
        return queries.findPublished(criteria, pageable);
    }
}
