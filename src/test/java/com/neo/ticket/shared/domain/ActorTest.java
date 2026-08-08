package com.neo.ticket.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.error.InvariantViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Set;

@DisplayName("Actor")
class ActorTest {

    private final UserId self = UserId.newId();
    private final UserId somebodyElse = UserId.newId();

    @Test
    @DisplayName("given an owner, when asked about their own resource, then they may administer it")
    void ownersMayActOnTheirOwn() {
        Actor owner = new Actor(self, Set.of(Role.ORGANIZER));

        assertThat(owner.mayAdminister(self)).isTrue();
        assertThat(owner.mayAdminister(somebodyElse)).isFalse();
    }

    @Test
    @DisplayName("given an administrator, when asked about anyone's resource, then they may administer it")
    void administratorsMayActOnAnything() {
        Actor admin = new Actor(self, Set.of(Role.ADMIN));

        assertThat(admin.isAdmin()).isTrue();
        assertThat(admin.mayAdminister(somebodyElse)).isTrue();
    }

    @Test
    @DisplayName("given a customer, when asked, then they are not an administrator")
    void customersAreNotAdministrators() {
        Actor customer = new Actor(self, Set.of(Role.CUSTOMER));

        assertThat(customer.isAdmin()).isFalse();
        assertThat(customer.hasRole(Role.CUSTOMER)).isTrue();
        assertThat(customer.hasRole(Role.ORGANIZER)).isFalse();
    }

    @Test
    @DisplayName("given a mutable role set, when the actor is built, then later edits do not affect it")
    void defendsItsRolesAgainstOutsideMutation() {
        Set<Role> mutable = new java.util.HashSet<>(Set.of(Role.CUSTOMER));
        Actor actor = new Actor(self, mutable);

        mutable.add(Role.ADMIN);

        assertThat(actor.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("given no user id, when built, then it is rejected")
    void requiresAUserId() {
        assertThatThrownBy(() -> new Actor(null, Set.of(Role.CUSTOMER)))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    @DisplayName("given an authority string, when converted, then the ROLE_ prefix is handled either way")
    void convertsAuthorityStrings() {
        assertThat(Role.ADMIN.authority()).isEqualTo("ROLE_ADMIN");
        assertThat(Role.fromAuthority("ROLE_ORGANIZER")).isEqualTo(Role.ORGANIZER);
        assertThat(Role.fromAuthority("CUSTOMER")).isEqualTo(Role.CUSTOMER);
    }
}
