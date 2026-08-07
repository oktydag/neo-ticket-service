package com.neo.ticket.reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.neo.ticket.reservation.domain.valueobject.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("ReservationStatus")
class ReservationStatusTest {

    @Test
    @DisplayName("given PENDING, when asked, then it may be confirmed or cancelled")
    void pendingCanGoEitherWay() {
        assertThat(ReservationStatus.PENDING.canTransitionTo(ReservationStatus.CONFIRMED)).isTrue();
        assertThat(ReservationStatus.PENDING.canTransitionTo(ReservationStatus.CANCELLED)).isTrue();
    }

    @Test
    @DisplayName("given CONFIRMED, when asked, then it may only be cancelled")
    void confirmedCanOnlyBeCancelled() {
        assertThat(ReservationStatus.CONFIRMED.canTransitionTo(ReservationStatus.CANCELLED)).isTrue();
        assertThat(ReservationStatus.CONFIRMED.canTransitionTo(ReservationStatus.PENDING)).isFalse();
    }

    @ParameterizedTest(name = "to {0}")
    @EnumSource(ReservationStatus.class)
    @DisplayName("given CANCELLED, when asked about any target, then nothing is permitted")
    void cancelledIsTerminal(ReservationStatus target) {
        assertThat(ReservationStatus.CANCELLED.canTransitionTo(target)).isFalse();
        assertThat(ReservationStatus.CANCELLED.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("given each status, when asked whether it holds seats, then only CANCELLED does not")
    void onlyCancelledReleasesSeats() {
        assertThat(ReservationStatus.PENDING.holdsSeats()).isTrue();
        assertThat(ReservationStatus.CONFIRMED.holdsSeats()).isTrue();
        assertThat(ReservationStatus.CANCELLED.holdsSeats()).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ReservationStatus.class)
    @DisplayName("given any status, when asked, then it never permits a transition to itself")
    void noSelfTransitions(ReservationStatus status) {
        assertThat(status.canTransitionTo(status)).isFalse();
    }
}
