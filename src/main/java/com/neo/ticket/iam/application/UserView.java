package com.neo.ticket.iam.application;

import com.neo.ticket.iam.domain.User;
import com.neo.ticket.shared.domain.valueobject.Role;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserView(UUID id, String email, Set<Role> roles, Instant createdAt, Instant lastLoginAt) {

    public static UserView from(User user) {
        return new UserView(
                user.id().value(),
                user.email().value(),
                user.roles(),
                user.createdAt(),
                user.lastLoginAt());
    }
}
