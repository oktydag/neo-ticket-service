package com.neo.ticket.shared.domain.valueobject;

import com.neo.ticket.shared.domain.Invariants;
import java.util.Set;

public record Actor(UserId userId, Set<Role> roles) {

    public Actor {
        Invariants.requirePresent(userId, "actor.userId");
        Invariants.requirePresent(roles, "actor.roles");
        roles = Set.copyOf(roles);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    public boolean is(UserId other) {
        return userId.equals(other);
    }

    public boolean mayAdminister(UserId ownerId) {
        return isAdmin() || is(ownerId);
    }
}
