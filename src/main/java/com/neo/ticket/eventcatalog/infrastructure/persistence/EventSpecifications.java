package com.neo.ticket.eventcatalog.infrastructure.persistence;

import com.neo.ticket.eventcatalog.domain.Event;
import com.neo.ticket.shared.domain.valueobject.UserId;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Locale;

final class EventSpecifications {

    private EventSpecifications() {
    }

    static Specification<Event> isPublished() {
        return (root, query, builder) -> builder.isTrue(root.get("published"));
    }

    static Specification<Event> ownedBy(UserId ownerId) {
        return (root, query, builder) -> builder.equal(root.get("ownerId"), ownerId.value());
    }

    static Specification<Event> startsAtOrAfter(Instant from) {
        return (root, query, builder) ->
                builder.greaterThanOrEqualTo(root.get("schedule").get("startsAt"), from);
    }

    static Specification<Event> startsAtOrBefore(Instant to) {
        return (root, query, builder) ->
                builder.lessThanOrEqualTo(root.get("schedule").get("startsAt"), to);
    }

    static Specification<Event> matchesText(String term) {
        String pattern = "%" + term.toLowerCase(Locale.ROOT).trim() + "%";
        return (root, query, builder) -> {
            Predicate titleMatches = builder.like(builder.lower(root.get("title")), pattern);
            Predicate venueMatches = builder.like(builder.lower(root.get("venue")), pattern);
            return builder.or(titleMatches, venueMatches);
        };
    }
}
