package com.neo.ticket.eventcatalog.api;

import com.neo.ticket.eventcatalog.application.EventView;
import com.neo.ticket.eventcatalog.application.query.EventSearchCriteria;
import com.neo.ticket.eventcatalog.application.query.handlers.DiscoverEventsHandler;
import com.neo.ticket.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@Tag(name = "Discovery", description = "Browsing published events without an account")
@SecurityRequirements
class PublicEventController {

    private static final Sort SOONEST_FIRST = Sort.by(Sort.Direction.ASC, "schedule.startsAt");

    private final DiscoverEventsHandler discoverEventsHandler;

    PublicEventController(DiscoverEventsHandler discoverEventsHandler) {
        this.discoverEventsHandler = discoverEventsHandler;
    }

    @GetMapping("/api/events/public")
    @Operation(summary = "Browse published events",
            description = """
                    Published events only, soonest first. Drafts are never returned here,
                    whoever is asking. All filters are optional.""")
    PageResponse<EventView> discover(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,

            @RequestParam(required = false, name = "q") String text,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PageResponse.from(discoverEventsHandler.handle(
                new EventSearchCriteria(from, to, text),
                PageResponse.toPageable(page, size, SOONEST_FIRST)));
    }
}
