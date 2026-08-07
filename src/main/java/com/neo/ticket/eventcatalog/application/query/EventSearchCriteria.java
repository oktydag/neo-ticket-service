package com.neo.ticket.eventcatalog.application.query;

import java.time.Instant;
import java.util.Optional;

public record EventSearchCriteria(Instant from, Instant to, String text) {

    public static EventSearchCriteria unfiltered() {
        return new EventSearchCriteria(null, null, null);
    }

    public Optional<Instant> fromInstant() {
        return Optional.ofNullable(from);
    }

    public Optional<Instant> toInstant() {
        return Optional.ofNullable(to);
    }

    public Optional<String> searchText() {
        return Optional.ofNullable(text).filter(value -> !value.isBlank());
    }
}
