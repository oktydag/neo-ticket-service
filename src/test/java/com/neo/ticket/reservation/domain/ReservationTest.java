package com.neo.ticket.reservation.domain;

import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.reservation.domain.valueobject.ReservationId;
import com.neo.ticket.reservation.domain.valueobject.ReservationStatus;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.error.*;
import com.neo.ticket.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.neo.ticket.testsupport.TestFixtures.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Reservation")
class ReservationTest {

    private EventId eventId;
    private UserId holder;
    private Actor holderActor;
    private Actor stranger;

    @BeforeEach
    void setUp() {
        eventId = EventId.newId();
        holder = UserId.newId();
        holderActor = customer(holder);
        stranger = customer(UserId.newId());
    }

    @Nested
    @DisplayName("placing")
    class Placing {

        @Test
        @DisplayName("given a seat count, when placed, then it starts PENDING and announces itself")
        void startsPending() {
            Reservation reservation = Reservation.place(ReservationId.newId(), eventId, holder, 2, NOW);

            assertThat(reservation.status()).isEqualTo(ReservationStatus.PENDING);
            assertThat(reservation.seats()).isEqualTo(2);
            assertThat(reservation.holdsSeats()).isTrue();
            assertThat(reservation.domainEvents()).singleElement().isInstanceOf(ReservationCreated.class);
        }

        @ParameterizedTest(name = "seats = {0}")
        @ValueSource(ints = {0, -1, Reservation.MAX_SEATS_PER_RESERVATION + 1})
        @DisplayName("given a seat count outside the allowed range, when placed, then it is rejected")
        void rejectsSeatCountsOutOfRange(int seats) {
            assertThatThrownBy(() -> Reservation.place(ReservationId.newId(), eventId, holder, seats, NOW))
                    .isInstanceOf(InvariantViolationException.class);
        }

        @Test
        @DisplayName("given the maximum seat count, when placed, then it is accepted")
        void acceptsTheMaximum() {
            Reservation reservation = Reservation.place(ReservationId.newId(), eventId, holder,
                    Reservation.MAX_SEATS_PER_RESERVATION, NOW);

            assertThat(reservation.seats()).isEqualTo(Reservation.MAX_SEATS_PER_RESERVATION);
        }
    }

    @Nested
    @DisplayName("confirming")
    class Confirming {

        @Test
        @DisplayName("given a pending reservation, when confirmed, then it becomes CONFIRMED and still holds its seats")
        void confirmsAPendingReservation() {
            Reservation reservation = pendingReservation(eventId, holder, 2);

            reservation.confirm(holderActor, NOW);

            assertThat(reservation.status()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(reservation.holdsSeats()).isTrue();
            assertThat(reservation.domainEvents()).singleElement().isInstanceOf(ReservationConfirmed.class);
        }

        @Test
        @DisplayName("given an already confirmed reservation, when confirmed again, then it is refused")
        void refusesToConfirmTwice() {
            Reservation reservation = pendingReservation(eventId, holder, 2);
            reservation.confirm(holderActor, NOW);

            assertThatThrownBy(() -> reservation.confirm(holderActor, NOW))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(errorCodeIs(ErrorCode.ILLEGAL_STATUS_TRANSITION))
                    .hasMessageContaining("already CONFIRMED");
        }

        @Test
        @DisplayName("given a cancelled reservation, when confirmed, then it is refused")
        void refusesToResurrectACancelledReservation() {
            Reservation reservation = pendingReservation(eventId, holder, 2);
            reservation.cancel(holderActor, NOW);

            assertThatThrownBy(() -> reservation.confirm(holderActor, NOW))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("cannot go from CANCELLED to CONFIRMED");
        }
    }

    @Nested
    @DisplayName("cancelling")
    class Cancelling {

        @Test
        @DisplayName("given a pending reservation, when cancelled, then it releases its seats")
        void cancelsAPendingReservation() {
            Reservation reservation = pendingReservation(eventId, holder, 2);

            reservation.cancel(holderActor, NOW);

            assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(reservation.holdsSeats()).isFalse();
            assertThat(reservation.domainEvents()).singleElement().isInstanceOf(ReservationCancelled.class);
        }

        @Test
        @DisplayName("given a confirmed reservation, when cancelled, then it is allowed")
        void cancelsAConfirmedReservation() {
            Reservation reservation = pendingReservation(eventId, holder, 2);
            reservation.confirm(holderActor, NOW);

            assertThatCode(() -> reservation.cancel(holderActor, NOW)).doesNotThrowAnyException();
            assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        @DisplayName("given a cancelled reservation, when cancelled again, then it is refused")
        void refusesToCancelTwice() {
            Reservation reservation = pendingReservation(eventId, holder, 2);
            reservation.cancel(holderActor, NOW);

            assertThatThrownBy(() -> reservation.cancel(holderActor, NOW))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(errorCodeIs(ErrorCode.ILLEGAL_STATUS_TRANSITION));
        }
    }

    @Nested
    @DisplayName("ownership")
    class Ownership {

        @Test
        @DisplayName("given another customer, when they act on the reservation, then it is refused")
        void refusesStrangers() {
            Reservation reservation = pendingReservation(eventId, holder, 2);

            assertThatThrownBy(() -> reservation.confirm(stranger, NOW))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .satisfies(errorCodeIs(ErrorCode.NOT_RESOURCE_OWNER));
            assertThatThrownBy(() -> reservation.cancel(stranger, NOW))
                    .isInstanceOf(ForbiddenOperationException.class);
        }

        @Test
        @DisplayName("given the ownership check fails, when it is refused, then the status is untouched")
        void leavesTheStatusAloneWhenRefused() {
            Reservation reservation = pendingReservation(eventId, holder, 2);

            assertThatThrownBy(() -> reservation.cancel(stranger, NOW))
                    .isInstanceOf(ForbiddenOperationException.class);

            assertThat(reservation.status()).isEqualTo(ReservationStatus.PENDING);
        }

        @Test
        @DisplayName("given an administrator, when they act on another's reservation, then it is allowed")
        void allowsAdministrators() {
            Reservation reservation = pendingReservation(eventId, holder, 2);

            assertThatCode(() -> reservation.cancel(TestFixtures.admin(), NOW)).doesNotThrowAnyException();
        }
    }

    private static java.util.function.Consumer<Throwable> errorCodeIs(ErrorCode expected) {
        return thrown -> assertThat(((DomainException) thrown).errorCode()).isEqualTo(expected);
    }
}
