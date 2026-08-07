package com.neo.ticket.audit.application;

import com.neo.ticket.audit.domain.AuditLog;
import com.neo.ticket.audit.domain.AuditLogRepository;
import com.neo.ticket.shared.domain.AuditableEvent;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.security.CurrentActorProvider;
import com.neo.ticket.shared.web.RequestMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AuditTrailListener")
class AuditTrailListenerTest {

    private static final Instant NOW = Instant.parse("2026-06-01T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UserId EVENT_ACTOR = UserId.of("11111111-1111-1111-1111-111111111111");
    private static final UserId CURRENT_ACTOR = UserId.of("22222222-2222-2222-2222-222222222222");

    private AuditLogRepository repository;
    private CurrentActorProvider currentActorProvider;
    private AuditTrailListener listener;

    @BeforeEach
    void setUp() {
        repository = mock(AuditLogRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        currentActorProvider = mock(CurrentActorProvider.class);
        when(currentActorProvider.find()).thenReturn(Optional.empty());
        listener = new AuditTrailListener(repository, currentActorProvider, FIXED_CLOCK);
    }

    @Nested
    @DisplayName("actor resolution")
    class ActorResolution {

        @Test
        @DisplayName("given the event carries an actor, when handled, then that actor is stored")
        void prefersActorFromEvent() {
            listener.on(eventWithActor(EVENT_ACTOR));

            assertThat(captureSaved().actorId()).isEqualTo(EVENT_ACTOR);
            verify(currentActorProvider, never()).find();
        }

        @Test
        @DisplayName("given the event has none, when a user is authenticated, then their id is stored")
        void fallsBackToTheCurrentActor() {
            when(currentActorProvider.find()).thenReturn(Optional.of(
                    new Actor(CURRENT_ACTOR, Set.of(Role.CUSTOMER))));

            listener.on(eventWithoutActor());

            assertThat(captureSaved().actorId()).isEqualTo(CURRENT_ACTOR);
        }

        @Test
        @DisplayName("given nobody is known, when handled, then the record is anonymous")
        void recordsAnAnonymousActorWhenUnknown() {
            listener.on(eventWithoutActor());

            assertThat(captureSaved().actorId()).isNull();
        }
    }

    @Nested
    @DisplayName("record content")
    class RecordContent {

        @Test
        @DisplayName("given the event, when handled, then action/type/id are copied verbatim")
        void copiesEventDescriptors() {
            listener.on(new StubEvent("user.registered", "user", "42", Optional.empty()));

            AuditLog captured = captureSaved();
            assertThat(captured.action()).isEqualTo("user.registered");
            assertThat(captured.resourceType()).isEqualTo("user");
            assertThat(captured.resourceId()).isEqualTo("42");
        }

        @Test
        @DisplayName("given a clock, when handled, then the stamp comes from it, not the event")
        void stampsUsingTheClock() {
            listener.on(eventWithoutActor());

            assertThat(captureSaved().createdAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("given no request context, when handled, then metadata falls back to unknown")
        void substitutesUnknownForMissingRequestContext() {
            listener.on(eventWithoutActor());

            AuditLog captured = captureSaved();
            assertThat(captured.ip()).isEqualTo(RequestMetadata.UNKNOWN);
            assertThat(captured.userAgent()).isEqualTo(RequestMetadata.UNKNOWN);
            assertThat(captured.requestId()).isEqualTo(RequestMetadata.UNKNOWN);
        }
    }

    @Nested
    @DisplayName("resilience")
    class Resilience {

        @Test
        @DisplayName("given the repository fails, when handled, then the caller is not disturbed")
        void swallowsRepositoryFailures() {
            when(repository.save(any())).thenThrow(new RuntimeException("db down"));

            assertThatCode(() -> listener.on(eventWithoutActor()))
                    .doesNotThrowAnyException();
        }
    }

    private AuditLog captureSaved() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private static StubEvent eventWithActor(UserId actorId) {
        return new StubEvent("a.b", "resource", "res-1", Optional.of(actorId));
    }

    private static StubEvent eventWithoutActor() {
        return new StubEvent("a.b", "resource", "res-1", Optional.empty());
    }

    private record StubEvent(String action, String resourceType, String resourceId,
                             Optional<UserId> actorId) implements AuditableEvent {

        @Override
        public Instant occurredAt() {
            return NOW;
        }
    }
}
