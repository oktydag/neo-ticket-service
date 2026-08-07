package com.neo.ticket.iam.domain;

import com.neo.ticket.iam.domain.valueobject.Email;
import com.neo.ticket.iam.domain.valueobject.PasswordHash;
import com.neo.ticket.shared.domain.AggregateRoot;
import com.neo.ticket.shared.domain.Invariants;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.error.BusinessRuleViolationException;
import com.neo.ticket.shared.error.ErrorCode;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends AggregateRoot<UserId> {

    public static final Set<Role> SELF_ASSIGNABLE_ROLES = Set.of(Role.CUSTOMER, Role.ORGANIZER);

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = Email.MAX_LENGTH)
    private Email email;

    @Column(name = "password_hash", nullable = false, length = PasswordHash.MAX_LENGTH)
    private PasswordHash passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = EnumSet.noneOf(Role.class);

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected User() {
    }

    private User(UserId id, Email email, PasswordHash passwordHash, Set<Role> roles, Instant createdAt) {
        this.id = Invariants.requirePresent(id, "user.id").value();
        this.email = Invariants.requirePresent(email, "user.email");
        this.passwordHash = Invariants.requirePresent(passwordHash, "user.passwordHash");
        this.createdAt = Invariants.requirePresent(createdAt, "user.createdAt");
        this.roles = validated(roles);
    }

    public static User register(UserId id, Email email, PasswordHash passwordHash,
                                Set<Role> requestedRoles, Instant now) {
        Set<Role> granted = validated(requestedRoles);
        if (!SELF_ASSIGNABLE_ROLES.containsAll(granted)) {
            throw new BusinessRuleViolationException(ErrorCode.ACCESS_DENIED,
                    "Roles %s cannot be granted through self-registration"
                            .formatted(difference(granted, SELF_ASSIGNABLE_ROLES)));
        }
        User user = new User(id, email, passwordHash, granted, now);
        user.raise(new UserRegistered(id, email, granted, now));
        return user;
    }

    public static User provision(UserId id, Email email, PasswordHash passwordHash,
                                 Set<Role> roles, Instant now) {
        return new User(id, email, passwordHash, roles, now);
    }

    public void recordSuccessfulLogin(Instant now) {
        this.lastLoginAt = Invariants.requirePresent(now, "loginAt");
    }

    public Actor toActor() {
        return new Actor(id(), roles);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    private static Set<Role> validated(Set<Role> roles) {
        Invariants.requirePresent(roles, "user.roles");
        Invariants.require(!roles.isEmpty(), "user must have at least one role");
        return EnumSet.copyOf(roles);
    }

    private static Set<Role> difference(Set<Role> requested, Set<Role> allowed) {
        EnumSet<Role> rejected = EnumSet.copyOf(requested);
        rejected.removeAll(allowed);
        return rejected;
    }

    @Override
    public UserId id() {
        return new UserId(id);
    }

    public Email email() {
        return email;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public Set<Role> roles() {
        return Set.copyOf(roles);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant lastLoginAt() {
        return lastLoginAt;
    }
}
