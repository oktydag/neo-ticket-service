package com.neo.ticket.eventcatalog.api.dto;

import com.neo.ticket.eventcatalog.domain.Event;
import com.neo.ticket.eventcatalog.domain.valueobject.Capacity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.Instant;

public record EventRequest(
        @NotBlank
        @Size(min = Event.TITLE_MIN_LENGTH, max = Event.TITLE_MAX_LENGTH)
        @Schema(example = "Spring Boot 4 Deep Dive")
        String title,

        @NotBlank
        @Size(min = Event.VENUE_MIN_LENGTH, max = Event.VENUE_MAX_LENGTH)
        @Schema(example = "Neo Arena, Hall B")
        String venue,

        @NotNull
        @Schema(description = "Start of the event. Must be in the future.",
                example = "2026-12-01T18:00:00Z")
        Instant startsAt,

        @NotNull
        @Schema(description = "End of the event. Must be after startsAt.",
                example = "2026-12-01T21:00:00Z")
        Instant endsAt,

        @Min(Capacity.MIN_TOTAL)
        @Max(Capacity.MAX_TOTAL)
        @Schema(description = "Total seats. Cannot be reduced below the seats already reserved.",
                example = "500")
        int capacity) {
}
