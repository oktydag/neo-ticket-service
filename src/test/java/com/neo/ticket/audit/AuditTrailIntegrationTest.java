package com.neo.ticket.audit;

import com.neo.ticket.audit.domain.AuditLog;
import com.neo.ticket.eventcatalog.application.EventView;
import com.neo.ticket.eventcatalog.application.command.CreateEventCommand;
import com.neo.ticket.eventcatalog.application.command.handlers.CreateEventHandler;
import com.neo.ticket.eventcatalog.application.command.handlers.PublishEventHandler;
import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.iam.application.command.LoginCommand;
import com.neo.ticket.iam.application.command.handlers.LoginHandler;
import com.neo.ticket.iam.domain.User;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.error.AuthenticationFailedException;
import com.neo.ticket.testsupport.IntegrationTest;
import com.neo.ticket.testsupport.TestAccounts;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@DisplayName("Audit trail")
class AuditTrailIntegrationTest {

    @Autowired
    private CreateEventHandler createEventHandler;

    @Autowired
    private PublishEventHandler publishEventHandler;

    @Autowired
    private LoginHandler loginHandler;

    @Autowired
    private TestAccounts accounts;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    @Transactional(readOnly = true)
    List<AuditLog> auditRecordsFor(String action) {
        return entityManager
                .createQuery("select a from AuditLog a where a.action = :action", AuditLog.class)
                .setParameter("action", action)
                .getResultList();
    }

    @Test
    @DisplayName("given an event is created and published, when it succeeds, then both actions are recorded")
    void recordsSuccessfulActions() {
        User organizerUser = accounts.create(Role.ORGANIZER);
        Actor organizer = organizerUser.toActor();
        Instant startsAt = clock.instant().plus(30, ChronoUnit.DAYS);

        EventView created = createEventHandler.handle(organizer, new CreateEventCommand(
                "Audited Conference", "Neo Arena, Hall B",
                startsAt, startsAt.plusSeconds(3600), 10));
        publishEventHandler.handle(organizer, new EventId(created.id()));

        assertThat(auditRecordsFor("EVENT_CREATED"))
                .anySatisfy(record -> {
                    assertThat(record.resourceId()).isEqualTo(created.id().toString());
                    assertThat(record.resourceType()).isEqualTo("Event");
                    assertThat(record.createdAt()).isNotNull();
                });
        assertThat(auditRecordsFor("EVENT_PUBLISHED"))
                .anySatisfy(record -> assertThat(record.resourceId())
                        .isEqualTo(created.id().toString()));
    }

    @Test
    @DisplayName("given a successful login, when it completes, then it is recorded against the user")
    void recordsSuccessfulLogins() {
        User user = accounts.create(Role.CUSTOMER);

        loginHandler.handle(new LoginCommand(user.email().value(), TestAccounts.PASSWORD));

        assertThat(auditRecordsFor("USER_LOGGED_IN"))
                .anySatisfy(record -> assertThat(record.actorId()).isEqualTo(user.id()));
    }

    @Test
    @DisplayName("given a rejected login, when the transaction is rolled back, "
            + "then the attempt is still recorded")
    void recordsFailedLoginsDespiteTheRejection() {
        User user = accounts.create(Role.CUSTOMER);

        assertThatThrownBy(() -> loginHandler.handle(
                new LoginCommand(user.email().value(), "the-wrong-password")))
                .isInstanceOf(AuthenticationFailedException.class);

        assertThat(auditRecordsFor("LOGIN_FAILED"))
                .as("losing this record would lose the evidence of a brute-force attempt")
                .anySatisfy(record -> {
                    assertThat(record.resourceId()).isEqualTo(user.email().value());
                    assertThat(record.resourceType()).isEqualTo("Authentication");
                    assertThat(record.actorId()).as("no identity was established").isNull();
                });
    }

    @Test
    @DisplayName("given an unknown address, when login is attempted, then the attempt is recorded")
    void recordsAttemptsAgainstUnknownAccounts() {
        assertThatThrownBy(() -> loginHandler.handle(
                new LoginCommand("ghost@neo.io", "some-long-password")))
                .isInstanceOf(AuthenticationFailedException.class);

        assertThat(auditRecordsFor("LOGIN_FAILED"))
                .anySatisfy(record -> assertThat(record.resourceId()).isEqualTo("ghost@neo.io"));
    }

    @Test
    @DisplayName("given any audit record, when written, then the transport metadata is filled in")
    void alwaysStoresTransportMetadata() {
        User user = accounts.create(Role.CUSTOMER);
        loginHandler.handle(new LoginCommand(user.email().value(), TestAccounts.PASSWORD));

        assertThat(auditRecordsFor("USER_LOGGED_IN"))
                .allSatisfy(record -> {
                    assertThat(record.ip()).isNotBlank();
                    assertThat(record.userAgent()).isNotBlank();
                    assertThat(record.requestId()).isNotBlank();
                });
    }
}
