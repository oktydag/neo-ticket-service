package com.neo.ticket.iam.application.command.handlers;

import com.neo.ticket.iam.application.UserView;
import com.neo.ticket.iam.application.command.RegisterUserCommand;
import com.neo.ticket.iam.domain.PasswordHasher;
import com.neo.ticket.iam.domain.User;
import com.neo.ticket.iam.domain.UserRepository;
import com.neo.ticket.iam.domain.valueobject.Email;
import com.neo.ticket.iam.domain.valueobject.RawPassword;
import com.neo.ticket.shared.application.DomainEventPublisher;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.error.BusinessRuleViolationException;
import com.neo.ticket.shared.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Set;

@Service
public class RegisterUserHandler {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    public RegisterUserHandler(UserRepository users,
                               PasswordHasher passwordHasher,
                               DomainEventPublisher domainEventPublisher,
                               Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.domainEventPublisher = domainEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public UserView handle(RegisterUserCommand command) {
        Email email = Email.of(command.email());
        if (users.existsByEmail(email)) {
            throw new BusinessRuleViolationException(ErrorCode.EMAIL_ALREADY_REGISTERED,
                    "An account already exists for this e-mail address");
        }

        Set<Role> requestedRoles = command.requestedRoles() == null || command.requestedRoles().isEmpty()
                ? Set.of(Role.CUSTOMER)
                : command.requestedRoles();

        User user = User.register(
                UserId.newId(),
                email,
                passwordHasher.hash(RawPassword.of(command.password())),
                requestedRoles,
                clock.instant());

        User saved = users.save(user);
        domainEventPublisher.publishEventsOf(user);
        return UserView.from(saved);
    }
}
