package com.neo.ticket.testsupport;

import com.neo.ticket.iam.domain.PasswordHasher;
import com.neo.ticket.iam.domain.User;
import com.neo.ticket.iam.domain.UserRepository;
import com.neo.ticket.iam.domain.valueobject.Email;
import com.neo.ticket.iam.domain.valueobject.RawPassword;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.domain.valueobject.UserId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

@Component
public class TestAccounts {

    public static final String PASSWORD = "integration-test-password";

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public TestAccounts(UserRepository users, PasswordHasher passwordHasher, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Transactional
    public User create(Role... roles) {
        String email = "user-%s@neo.io".formatted(UUID.randomUUID());
        return users.save(User.provision(
                UserId.newId(),
                Email.of(email),
                passwordHasher.hash(RawPassword.of(PASSWORD)),
                Set.of(roles),
                clock.instant()));
    }
}
