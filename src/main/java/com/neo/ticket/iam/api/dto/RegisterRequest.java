package com.neo.ticket.iam.api.dto;

import com.neo.ticket.iam.domain.valueobject.Email;
import com.neo.ticket.iam.domain.valueobject.RawPassword;
import com.neo.ticket.shared.domain.valueobject.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record RegisterRequest(
        @NotBlank
        @Size(max = Email.MAX_LENGTH)
        @Schema(example = "ada@neo.io")
        String email,

        @NotBlank
        @Size(min = RawPassword.MIN_LENGTH, max = RawPassword.MAX_LENGTH)
        @Schema(example = "correct-horse-battery-staple", minLength = RawPassword.MIN_LENGTH)
        String password,

        @Schema(description = "Roles to request. ADMIN cannot be self-assigned.",
                example = "[\"ORGANIZER\"]")
        Set<Role> requestedRoles) {
}
