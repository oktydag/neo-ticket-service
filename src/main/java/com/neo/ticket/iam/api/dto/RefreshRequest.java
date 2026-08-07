package com.neo.ticket.iam.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank
        @Schema(description = "The refresh token from the last login or refresh. Single use.")
        String refreshToken) {
}
