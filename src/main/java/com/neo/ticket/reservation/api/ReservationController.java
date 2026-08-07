package com.neo.ticket.reservation.api;

import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.idempotency.application.IdempotencyContext;
import com.neo.ticket.idempotency.application.IdempotencyGuard;
import com.neo.ticket.idempotency.application.IdempotentOutcome;
import com.neo.ticket.reservation.api.dto.CreateReservationRequest;
import com.neo.ticket.reservation.application.ReservationView;
import com.neo.ticket.reservation.application.command.handlers.CancelReservationHandler;
import com.neo.ticket.reservation.application.command.handlers.ConfirmReservationHandler;
import com.neo.ticket.reservation.application.command.handlers.ReserveSeatsHandler;
import com.neo.ticket.reservation.application.query.handlers.GetReservationHandler;
import com.neo.ticket.reservation.domain.valueobject.ReservationId;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.security.CurrentActorProvider;
import com.neo.ticket.shared.web.IdempotencyHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Tag(name = "Reservations", description = "Holding, confirming and cancelling seats")
class ReservationController {

    private static final String RESERVE_ENDPOINT = "POST /api/events/%s/reservations";

    private final ReserveSeatsHandler reserveSeatsHandler;
    private final ConfirmReservationHandler confirmReservationHandler;
    private final CancelReservationHandler cancelReservationHandler;
    private final GetReservationHandler getReservationHandler;
    private final IdempotencyGuard idempotencyGuard;
    private final CurrentActorProvider currentActor;

    ReservationController(ReserveSeatsHandler reserveSeatsHandler,
                          ConfirmReservationHandler confirmReservationHandler,
                          CancelReservationHandler cancelReservationHandler,
                          GetReservationHandler getReservationHandler,
                          IdempotencyGuard idempotencyGuard,
                          CurrentActorProvider currentActor) {
        this.reserveSeatsHandler = reserveSeatsHandler;
        this.confirmReservationHandler = confirmReservationHandler;
        this.cancelReservationHandler = cancelReservationHandler;
        this.getReservationHandler = getReservationHandler;
        this.idempotencyGuard = idempotencyGuard;
        this.currentActor = currentActor;
    }

    @PostMapping("/api/events/{eventId}/reservations")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Hold seats at an event",
            description = """
                    Creates a PENDING reservation and takes the seats out of the event's pool
                    immediately, so a customer completing checkout cannot lose them to someone
                    faster. Requires an Idempotency-Key: repeating the same key with the same
                    body returns the original reservation instead of creating a second one.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Seats held"),
            @ApiResponse(responseCode = "400",
                    description = "Idempotency-Key missing, or seat count out of range",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "No such event", content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "Not enough seats, event not open, or the key was reused "
                            + "with a different body",
                    content = @Content)
    })
    ResponseEntity<ReservationView> reserve(
            @PathVariable UUID eventId,

            @RequestHeader(IdempotencyHeaders.HEADER)
            @Parameter(description = "A unique value per logical request, e.g. a UUID. "
                    + "Reuse it verbatim when retrying.", required = true)
            String idempotencyKey,

            @Valid @RequestBody CreateReservationRequest request) {

        Actor actor = currentActor.require();
        IdempotencyContext context = new IdempotencyContext(
                idempotencyKey, RESERVE_ENDPOINT.formatted(eventId), actor.userId());

        IdempotentOutcome<ReservationView> outcome = idempotencyGuard.execute(
                context,
                request,
                HttpStatus.CREATED.value(),
                ReservationView.class,
                () -> reserveSeatsHandler.handle(actor, new EventId(eventId), request.seats()));

        return ResponseEntity.status(outcome.httpStatus())
                .header(IdempotencyHeaders.REPLAYED_HEADER, String.valueOf(outcome.replayed()))
                .body(outcome.value());
    }

    @PostMapping("/api/reservations/{id}/confirm")
    @Operation(summary = "Confirm a pending reservation",
            description = "The seats were already held when the reservation was placed, "
                    + "so this cannot fail for lack of capacity.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation confirmed"),
            @ApiResponse(responseCode = "403",
                    description = "Reservation belongs to another customer", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such reservation", content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "Reservation is already confirmed or cancelled", content = @Content)
    })
    ReservationView confirm(@PathVariable UUID id) {
        return confirmReservationHandler.handle(currentActor.require(), new ReservationId(id));
    }

    @PostMapping("/api/reservations/{id}/cancel")
    @Operation(summary = "Cancel a reservation",
            description = "Returns the seats to the event's pool. Cancellation is final.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation cancelled"),
            @ApiResponse(responseCode = "403",
                    description = "Reservation belongs to another customer", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such reservation", content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "Reservation is already cancelled", content = @Content)
    })
    ReservationView cancel(@PathVariable UUID id) {
        return cancelReservationHandler.handle(currentActor.require(), new ReservationId(id));
    }

    @GetMapping("/api/reservations/{id}")
    @Operation(summary = "Fetch one reservation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The reservation"),
            @ApiResponse(responseCode = "403",
                    description = "Reservation belongs to another customer", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such reservation", content = @Content)
    })
    ReservationView getOne(@PathVariable UUID id) {
        return getReservationHandler.handle(currentActor.require(), new ReservationId(id));
    }
}
