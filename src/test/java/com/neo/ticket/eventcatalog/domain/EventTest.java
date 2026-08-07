package com.neo.ticket.eventcatalog.domain;
import static com.neo.ticket.testsupport.TestFixtures.NOW;
import static com.neo.ticket.testsupport.TestFixtures.draftEvent;
import static com.neo.ticket.testsupport.TestFixtures.futureSchedule;
import static com.neo.ticket.testsupport.TestFixtures.organizer;
import static com.neo.ticket.testsupport.TestFixtures.publishedEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.eventcatalog.domain.valueobject.EventSchedule;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.error.BusinessRuleViolationException;
import com.neo.ticket.shared.error.DomainException;
import com.neo.ticket.shared.error.ErrorCode;
import com.neo.ticket.shared.error.ForbiddenOperationException;
import com.neo.ticket.shared.error.InvariantViolationException;
import com.neo.ticket.testsupport.TestFixtures;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
@DisplayName("Event")
class EventTest {

    private UserId owner;
    private Actor ownerActor;
    private Actor otherOrganizer;

    @BeforeEach
    void setUp() {
        owner = UserId.newId();
        ownerActor = organizer(owner);
        otherOrganizer = organizer(UserId.newId());
    }

    @Nested
    @DisplayName("creating a draft")
    class CreatingADraft {

        @Test
        @DisplayName("given valid details, when created, then it is an unpublished event owned by the caller")
        void createsAnUnpublishedEvent() {
            Event event = draftEvent(owner, 100);

            assertThat(event.isPublished()).isFalse();
            assertThat(event.publishedAt()).isNull();
            assertThat(event.ownerId()).isEqualTo(owner);
            assertThat(event.capacity().total()).isEqualTo(100);
            assertThat(event.domainEvents()).singleElement().isInstanceOf(EventCreated.class);
        }

        @Test
        @DisplayName("given a start date in the past, when created, then it is rejected")
        void rejectsAStartInThePast() {
            EventSchedule past = EventSchedule.of(NOW.minus(2, ChronoUnit.DAYS), NOW.minus(1, ChronoUnit.DAYS));

            assertThatThrownBy(() -> Event.createDraft(EventId.newId(), owner, "Past Event",
                    "Neo Arena, Hall B", past, 10, NOW))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("startsAt must be in the future");
        }

        @ParameterizedTest(name = "title = \"{0}\"")
        @ValueSource(strings = {"", "  ", "ab"})
        @DisplayName("given a title that is blank or too short, when created, then it is rejected")
        void rejectsUnusableTitles(String title) {
            assertThatThrownBy(() -> Event.createDraft(EventId.newId(), owner, title,
                    "Neo Arena, Hall B", futureSchedule(), 10, NOW))
                    .isInstanceOf(InvariantViolationException.class);
        }

        @Test
        @DisplayName("given a title longer than the limit, when created, then it is rejected")
        void rejectsAnOverlongTitle() {
            String tooLong = "x".repeat(Event.TITLE_MAX_LENGTH + 1);

            assertThatThrownBy(() -> Event.createDraft(EventId.newId(), owner, tooLong,
                    "Neo Arena, Hall B", futureSchedule(), 10, NOW))
                    .isInstanceOf(InvariantViolationException.class);
        }

        @Test
        @DisplayName("given a padded title, when created, then it is stored trimmed")
        void normalisesWhitespace() {
            Event event = Event.createDraft(EventId.newId(), owner, "  Padded Title  ",
                    "  Neo Arena, Hall B  ", futureSchedule(), 10, NOW);

            assertThat(event.title()).isEqualTo("Padded Title");
            assertThat(event.venue()).isEqualTo("Neo Arena, Hall B");
        }
    }

    @Nested
    @DisplayName("publishing")
    class Publishing {

        @Test
        @DisplayName("given a draft, when the owner publishes it, then it becomes visible and reservable")
        void publishesADraft() {
            Event event = draftEvent(owner, 10);
            event.drainDomainEvents();

            event.publish(ownerActor, NOW);

            assertThat(event.isPublished()).isTrue();
            assertThat(event.publishedAt()).isEqualTo(NOW);
            assertThat(event.isOpenForReservations(NOW)).isTrue();
            assertThat(event.domainEvents()).singleElement().isInstanceOf(EventPublished.class);
        }

        @Test
        @DisplayName("given an already published event, when published again, then it is refused")
        void refusesToPublishTwice() {
            Event event = publishedEvent(owner, 10);

            assertThatThrownBy(() -> event.publish(ownerActor, NOW))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(errorCodeIs(ErrorCode.EVENT_ALREADY_PUBLISHED));
        }

        @Test
        @DisplayName("given the event has already started, when publishing, then it is refused")
        void refusesToPublishSomethingAlreadyUnderWay() {
            Event event = draftEvent(owner, 10);

            assertThatThrownBy(() -> event.publish(ownerActor, futureSchedule().startsAt().plusSeconds(1)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("already started");
        }

        @Test
        @DisplayName("given a different organizer, when publishing, then it is refused")
        void refusesAnotherOrganizer() {
            Event event = draftEvent(owner, 10);

            assertThatThrownBy(() -> event.publish(otherOrganizer, NOW))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .satisfies(errorCodeIs(ErrorCode.NOT_RESOURCE_OWNER));
        }

        @Test
        @DisplayName("given an administrator, when publishing another's event, then it is allowed")
        void allowsAdministrators() {
            Event event = draftEvent(owner, 10);

            assertThatCode(() -> event.publish(TestFixtures.admin(), NOW)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("editing a draft")
    class EditingADraft {

        @Test
        @DisplayName("given a draft, when edited, then every field including the schedule may change")
        void allowsAnyChange() {
            Event event = draftEvent(owner, 10);
            EventSchedule moved = EventSchedule.of(NOW.plus(60, ChronoUnit.DAYS),
                    NOW.plus(60, ChronoUnit.DAYS).plusSeconds(3600));

            event.update(ownerActor, "New Title", "New Venue Name", moved, 5, NOW);

            assertThat(event.title()).isEqualTo("New Title");
            assertThat(event.schedule()).isEqualTo(moved);
            assertThat(event.capacity().total()).isEqualTo(5);
        }

        @Test
        @DisplayName("given a schedule moved into the past, when edited, then it is refused")
        void refusesToMoveADraftIntoThePast() {
            Event event = draftEvent(owner, 10);
            EventSchedule past = EventSchedule.of(NOW.minus(5, ChronoUnit.DAYS), NOW.minus(4, ChronoUnit.DAYS));

            assertThatThrownBy(() -> event.update(ownerActor, "Title", "Venue Name", past, 10, NOW))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    @Nested
    @DisplayName("editing a published event")
    class EditingAPublishedEvent {

        @Test
        @DisplayName("given a published event, when the title is corrected, then it is allowed")
        void allowsCorrectingTheTitle() {
            Event event = publishedEvent(owner, 10);

            event.update(ownerActor, "Corrected Title", "Neo Arena, Hall B", event.schedule(), 10, NOW);

            assertThat(event.title()).isEqualTo("Corrected Title");
        }

        @Test
        @DisplayName("given a published event, when the schedule is moved, then it is refused")
        void freezesTheSchedule() {
            Event event = publishedEvent(owner, 10);
            EventSchedule moved = EventSchedule.of(NOW.plus(90, ChronoUnit.DAYS),
                    NOW.plus(90, ChronoUnit.DAYS).plusSeconds(3600));

            assertThatThrownBy(() -> event.update(ownerActor, "Title", "Venue Name", moved, 10, NOW))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(errorCodeIs(ErrorCode.EVENT_IMMUTABLE_AFTER_PUBLISH));
        }

        @Test
        @DisplayName("given a published event, when capacity grows, then it is allowed")
        void allowsCapacityToGrow() {
            Event event = publishedEvent(owner, 10);

            event.update(ownerActor, "Title", "Venue Name", event.schedule(), 25, NOW);

            assertThat(event.capacity().total()).isEqualTo(25);
        }

        @Test
        @DisplayName("given a published event, when capacity shrinks, then it is refused")
        void refusesToShrinkCapacity() {
            Event event = publishedEvent(owner, 10);

            assertThatThrownBy(() -> event.update(ownerActor, "Title", "Venue Name", event.schedule(), 9, NOW))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(errorCodeIs(ErrorCode.EVENT_IMMUTABLE_AFTER_PUBLISH));
        }
    }

    @Nested
    @DisplayName("reserving seats")
    class ReservingSeats {

        @Test
        @DisplayName("given a published event with room, when seats are reserved, then the remainder drops")
        void takesSeatsOutOfThePool() {
            Event event = publishedEvent(owner, 10);

            event.reserveSeats(4, NOW);

            assertThat(event.capacity().reserved()).isEqualTo(4);
            assertThat(event.capacity().remaining()).isEqualTo(6);
        }

        @Test
        @DisplayName("given an unpublished event, when seats are reserved, then it is refused")
        void refusesReservationsOnADraft() {
            Event event = draftEvent(owner, 10);

            assertThatThrownBy(() -> event.reserveSeats(1, NOW))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(errorCodeIs(ErrorCode.EVENT_NOT_PUBLISHED));
        }

        @Test
        @DisplayName("given the event has started, when seats are reserved, then it is refused")
        void refusesReservationsOnceUnderWay() {
            Event event = publishedEvent(owner, 10);

            assertThatThrownBy(() -> event.reserveSeats(1, event.schedule().startsAt().plusSeconds(1)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(errorCodeIs(ErrorCode.EVENT_NOT_RESERVABLE));
        }

        @Test
        @DisplayName("given a full event, when more seats are reserved, then it is refused")
        void refusesToOversell() {
            Event event = publishedEvent(owner, 2);
            event.reserveSeats(2, NOW);

            assertThatThrownBy(() -> event.reserveSeats(1, NOW))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(errorCodeIs(ErrorCode.INSUFFICIENT_CAPACITY));
        }

        @Test
        @DisplayName("given a sold-out event, when asked, then it reports itself closed")
        void reportsItselfClosedWhenSoldOut() {
            Event event = publishedEvent(owner, 1);
            event.reserveSeats(1, NOW);

            assertThat(event.isOpenForReservations(NOW)).isFalse();
        }

        @Test
        @DisplayName("given reserved seats, when they are released, then they become available again")
        void returnsReleasedSeats() {
            Event event = publishedEvent(owner, 10);
            event.reserveSeats(4, NOW);

            event.releaseSeats(3, NOW);

            assertThat(event.capacity().remaining()).isEqualTo(9);
        }
    }

    @Nested
    @DisplayName("visibility")
    class Visibility {

        @Test
        @DisplayName("given a draft, when viewed by a stranger, then it is invisible")
        void hidesDraftsFromStrangers() {
            Event event = draftEvent(owner, 10);

            assertThat(event.isVisibleTo(otherOrganizer)).isFalse();
            assertThat(event.isVisibleTo(null)).isFalse();
        }

        @Test
        @DisplayName("given a draft, when viewed by its owner or an admin, then it is visible")
        void showsDraftsToTheirOwner() {
            Event event = draftEvent(owner, 10);

            assertThat(event.isVisibleTo(ownerActor)).isTrue();
            assertThat(event.isVisibleTo(TestFixtures.admin())).isTrue();
        }

        @Test
        @DisplayName("given a published event, when viewed by anyone, then it is visible")
        void showsPublishedEventsToEveryone() {
            Event event = publishedEvent(owner, 10);

            assertThat(event.isVisibleTo(otherOrganizer)).isTrue();
            assertThat(event.isVisibleTo(null)).isTrue();
        }
    }

    private static java.util.function.Consumer<Throwable> errorCodeIs(ErrorCode expected) {
        return thrown -> assertThat(((DomainException) thrown).errorCode()).isEqualTo(expected);
    }
}
