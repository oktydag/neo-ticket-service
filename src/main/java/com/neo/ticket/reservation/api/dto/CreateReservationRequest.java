package com.neo.ticket.reservation.api.dto;

import com.neo.ticket.reservation.domain.Reservation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateReservationRequest(
        @Min(Reservation.MIN_SEATS_PER_RESERVATION)
        @Max(Reservation.MAX_SEATS_PER_RESERVATION)
        @Schema(description = "Seats to hold, at most "
                + Reservation.MAX_SEATS_PER_RESERVATION + " per reservation.", example = "2")
        int seats) {
}
