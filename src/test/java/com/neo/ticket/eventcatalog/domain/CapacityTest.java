package com.neo.ticket.eventcatalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neo.ticket.eventcatalog.domain.valueobject.Capacity;
import com.neo.ticket.shared.error.BusinessRuleViolationException;
import com.neo.ticket.shared.error.ErrorCode;
import com.neo.ticket.shared.error.InvariantViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Capacity")
class CapacityTest {

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("given a total, when created, then nothing is reserved yet")
        void startsEmpty() {
            Capacity capacity = Capacity.of(100);

            assertThat(capacity.total()).isEqualTo(100);
            assertThat(capacity.reserved()).isZero();
            assertThat(capacity.remaining()).isEqualTo(100);
            assertThat(capacity.isSoldOut()).isFalse();
        }

        @ParameterizedTest(name = "total = {0}")
        @ValueSource(ints = {0, -1, Capacity.MAX_TOTAL + 1})
        @DisplayName("given a total outside the allowed range, when created, then it is rejected")
        void rejectsTotalsOutOfRange(int total) {
            assertThatThrownBy(() -> Capacity.of(total))
                    .isInstanceOf(InvariantViolationException.class);
        }

        @Test
        @DisplayName("given more reserved than total, when created, then it is rejected")
        void rejectsReservedAboveTotal() {
            assertThatThrownBy(() -> Capacity.of(10, 11))
                    .isInstanceOf(InvariantViolationException.class)
                    .hasMessageContaining("must not exceed capacity");
        }
    }

    @Nested
    @DisplayName("reserving")
    class Reserving {

        @Test
        @DisplayName("given free seats, when reserving, then the remainder drops by that many")
        void reservesWithinCapacity() {
            Capacity capacity = Capacity.of(10).reserve(3);

            assertThat(capacity.reserved()).isEqualTo(3);
            assertThat(capacity.remaining()).isEqualTo(7);
        }

        @Test
        @DisplayName("given exactly enough seats, when reserving all of them, then it succeeds and sells out")
        void reservesTheLastSeats() {
            Capacity capacity = Capacity.of(5).reserve(5);

            assertThat(capacity.remaining()).isZero();
            assertThat(capacity.isSoldOut()).isTrue();
        }

        @Test
        @DisplayName("given one seat too few, when reserving, then it is refused with the true remainder")
        void refusesToOversellByOne() {
            Capacity capacity = Capacity.of(5).reserve(4);

            assertThatThrownBy(() -> capacity.reserve(2))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(thrown -> {
                        BusinessRuleViolationException violation = (BusinessRuleViolationException) thrown;
                        assertThat(violation.errorCode()).isEqualTo(ErrorCode.INSUFFICIENT_CAPACITY);
                        assertThat(violation.details()).containsEntry("remainingSeats", 1);
                    });
        }

        @Test
        @DisplayName("given a refused reservation, when it fails, then the original is left untouched")
        void leavesTheOriginalIntactOnFailure() {
            Capacity capacity = Capacity.of(5).reserve(4);

            assertThatThrownBy(() -> capacity.reserve(2)).isInstanceOf(RuntimeException.class);

            assertThat(capacity.reserved()).isEqualTo(4);
            assertThat(capacity.remaining()).isEqualTo(1);
        }

        @ParameterizedTest(name = "seats = {0}")
        @ValueSource(ints = {0, -1})
        @DisplayName("given a non-positive seat count, when reserving, then it is rejected")
        void rejectsNonPositiveSeatCounts(int seats) {
            assertThatThrownBy(() -> Capacity.of(10).reserve(seats))
                    .isInstanceOf(InvariantViolationException.class);
        }
    }

    @Nested
    @DisplayName("releasing")
    class Releasing {

        @Test
        @DisplayName("given reserved seats, when released, then they return to the pool")
        void returnsSeatsToThePool() {
            Capacity capacity = Capacity.of(10).reserve(4).release(3);

            assertThat(capacity.reserved()).isEqualTo(1);
            assertThat(capacity.remaining()).isEqualTo(9);
        }

        @Test
        @DisplayName("given more seats than are held, when releasing, then it is rejected")
        void refusesToReleaseMoreThanHeld() {
            assertThatThrownBy(() -> Capacity.of(10).reserve(2).release(3))
                    .isInstanceOf(InvariantViolationException.class)
                    .hasMessageContaining("cannot release");
        }
    }

    @Nested
    @DisplayName("resizing")
    class Resizing {

        @Test
        @DisplayName("given a larger total, when resized, then reservations are preserved")
        void growsWithoutDisturbingReservations() {
            Capacity capacity = Capacity.of(10).reserve(4).resizeTo(20);

            assertThat(capacity.total()).isEqualTo(20);
            assertThat(capacity.reserved()).isEqualTo(4);
            assertThat(capacity.remaining()).isEqualTo(16);
        }

        @Test
        @DisplayName("given a total at least as large as the reservations, when shrinking, then it succeeds")
        void shrinksDownToTheReservedCount() {
            Capacity capacity = Capacity.of(10).reserve(4).resizeTo(4);

            assertThat(capacity.total()).isEqualTo(4);
            assertThat(capacity.isSoldOut()).isTrue();
        }

        @Test
        @DisplayName("given a total below the reservations, when shrinking, then it is refused")
        void refusesToStrandExistingReservations() {
            Capacity capacity = Capacity.of(10).reserve(4);

            assertThatThrownBy(() -> capacity.resizeTo(3))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(thrown -> assertThat(((BusinessRuleViolationException) thrown).errorCode())
                            .isEqualTo(ErrorCode.CAPACITY_BELOW_RESERVED));
        }
    }

    @Test
    @DisplayName("given the same numbers, when compared, then two instances are equal")
    void comparesByValue() {
        assertThat(Capacity.of(10, 3)).isEqualTo(Capacity.of(10, 3));
        assertThat(Capacity.of(10, 3)).hasSameHashCodeAs(Capacity.of(10, 3));
        assertThat(Capacity.of(10, 3)).isNotEqualTo(Capacity.of(10, 4));
    }
}
