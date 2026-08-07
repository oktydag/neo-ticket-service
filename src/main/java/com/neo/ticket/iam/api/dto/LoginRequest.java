package com.neo.ticket.iam.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Schema(example = "organizer@neo.io") String email,
        @NotBlank @Schema(example = "neo-dev-password") String password) {
}
