package com.neo.ticket.eventcatalog.application.query.handlers;

import com.neo.ticket.eventcatalog.application.EventCatalogQueries;
import com.neo.ticket.eventcatalog.application.EventView;
import com.neo.ticket.eventcatalog.application.query.EventSearchCriteria;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.error.ErrorCode;
import com.neo.ticket.shared.error.ForbiddenOperationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ListEventsHandler {

    private final EventCatalogQueries queries;

    public ListEventsHandler(EventCatalogQueries queries) {
        this.queries = queries;
    }

    public Page<EventView> handle(Actor actor, UserId requestedOwnerId,
                                  EventSearchCriteria criteria, Pageable pageable) {
        Optional<UserId> ownerFilter = Optional.ofNullable(requestedOwnerId);

        if (actor.isAdmin()) {
            return ownerFilter
                    .map(ownerId -> queries.findOwnedBy(ownerId, criteria, pageable))
                    .orElseGet(() -> queries.findAll(criteria, pageable));
        }

        if (actor.hasRole(Role.ORGANIZER)) {
            ownerFilter.filter(ownerId -> !actor.is(ownerId)).ifPresent(ownerId -> {
                throw new ForbiddenOperationException(ErrorCode.NOT_RESOURCE_OWNER,
                        "Organizers may only list their own events");
            });
            return queries.findOwnedBy(actor.userId(), criteria, pageable);
        }

        return queries.findPublished(criteria, pageable);
    }
}
