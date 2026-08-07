package com.neo.ticket.iam.application.command;

import com.neo.ticket.shared.domain.valueobject.Role;
import java.util.Set;

public record RegisterUserCommand(String email, String password, Set<Role> requestedRoles) {
}
