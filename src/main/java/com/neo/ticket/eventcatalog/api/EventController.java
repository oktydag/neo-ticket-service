package com.neo.ticket.eventcatalog.api;

import com.neo.ticket.eventcatalog.api.dto.EventRequest;
import com.neo.ticket.eventcatalog.application.EventView;
import com.neo.ticket.eventcatalog.application.command.CreateEventCommand;
import com.neo.ticket.eventcatalog.application.command.UpdateEventCommand;
import com.neo.ticket.eventcatalog.application.command.handlers.CreateEventHandler;
import com.neo.ticket.eventcatalog.application.command.handlers.PublishEventHandler;
import com.neo.ticket.eventcatalog.application.command.handlers.UpdateEventHandler;
import com.neo.ticket.eventcatalog.application.query.EventSearchCriteria;
import com.neo.ticket.eventcatalog.application.query.handlers.GetEventHandler;
import com.neo.ticket.eventcatalog.application.query.handlers.ListEventsHandler;
import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.security.CurrentActorProvider;
import com.neo.ticket.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Creating, editing and publishing events")
class EventController {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final CreateEventHandler createEventHandler;
    private final UpdateEventHandler updateEventHandler;
    private final PublishEventHandler publishEventHandler;
    private final ListEventsHandler listEventsHandler;
    private final GetEventHandler getEventHandler;
    private final CurrentActorProvider currentActor;

    EventController(CreateEventHandler createEventHandler,
                    UpdateEventHandler updateEventHandler,
                    PublishEventHandler publishEventHandler,
                    ListEventsHandler listEventsHandler,
                    GetEventHandler getEventHandler,
                    CurrentActorProvider currentActor) {
        this.createEventHandler = createEventHandler;
        this.updateEventHandler = updateEventHandler;
        this.publishEventHandler = publishEventHandler;
        this.listEventsHandler = listEventsHandler;
        this.getEventHandler = getEventHandler;
        this.currentActor = currentActor;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Create a draft event",
            description = "The event is owned by the caller and is not visible to customers until published.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Draft created"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @ApiResponse(responseCode = "403", description = "Caller is not an organizer", content = @Content)
    })
    ResponseEntity<EventView> create(@Valid @RequestBody EventRequest request) {
        EventView created = createEventHandler.handle(
                currentActor.require(),
                new CreateEventCommand(request.title(), request.venue(),
                        request.startsAt(), request.endsAt(), request.capacity()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Replace an event's details",
            description = """
                    Drafts are freely editable. Once published, the schedule is frozen and the
                    capacity may only grow, since customers have already reserved against those terms.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event updated"),
            @ApiResponse(responseCode = "403", description = "Caller does not own the event", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such event", content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "Change is not allowed after publication, or a concurrent edit won",
                    content = @Content)
    })
    EventView update(@PathVariable UUID id, @Valid @RequestBody EventRequest request) {
        return updateEventHandler.handle(
                currentActor.require(),
                new EventId(id),
                new UpdateEventCommand(request.title(), request.venue(),
                        request.startsAt(), request.endsAt(), request.capacity()));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Publish a draft event",
            description = "Makes the event visible to customers and open for reservations. Not reversible.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event published"),
            @ApiResponse(responseCode = "403", description = "Caller does not own the event", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such event", content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "Already published, or the event has already started", content = @Content)
    })
    EventView publish(@PathVariable UUID id) {
        return publishEventHandler.handle(currentActor.require(), new EventId(id));
    }

    @GetMapping
    @Operation(summary = "List events",
            description = """
                    Administrators see every event. Organizers see their own, drafts included,
                    and cannot request another owner's. Everyone else sees published events only.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of events"),
            @ApiResponse(responseCode = "403",
                    description = "An organizer asked for another owner's events", content = @Content)
    })
    PageResponse<EventView> list(@RequestParam(required = false) UUID ownerId,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                 @RequestParam(required = false, name = "q") String text,
                                 @RequestParam(required = false) Integer page,
                                 @RequestParam(required = false) Integer size) {
        Actor actor = currentActor.require();
        return PageResponse.from(listEventsHandler.handle(
                actor,
                ownerId == null ? null : new UserId(ownerId),
                new EventSearchCriteria(from, to, text),
                PageResponse.toPageable(page, size, NEWEST_FIRST)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one event",
            description = "An unpublished event is reported as missing to anyone but its owner.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The event"),
            @ApiResponse(responseCode = "404",
                    description = "No such event, or it is a draft the caller does not own",
                    content = @Content)
    })
    EventView getOne(@PathVariable UUID id) {
        return getEventHandler.handle(currentActor.require(), new EventId(id));
    }
}
