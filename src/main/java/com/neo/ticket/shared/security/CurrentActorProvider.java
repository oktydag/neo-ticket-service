package com.neo.ticket.shared.security;

import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.error.AuthenticationFailedException;
import com.neo.ticket.shared.error.ErrorCode;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CurrentActorProvider {

    public Optional<Actor> find() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return toActor(authentication);
    }

    public Actor require() {
        return find().orElseThrow(() -> new AuthenticationFailedException(
                ErrorCode.AUTHENTICATION_REQUIRED, "This operation requires an authenticated user"));
    }

    private static Optional<Actor> toActor(Authentication authentication) {
        Set<Role> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(Role.AUTHORITY_PREFIX))
                .map(CurrentActorProvider::toRoleOrNull)
                .filter(role -> role != null)
                .collect(Collectors.toUnmodifiableSet());

        if (roles.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Actor(UserId.of(authentication.getName()), roles));
        } catch (IllegalArgumentException malformedSubject) {
            return Optional.empty();
        }
    }

    private static Role toRoleOrNull(String authority) {
        try {
            return Role.fromAuthority(authority);
        } catch (IllegalArgumentException unknownRole) {
            return null;
        }
    }
}
