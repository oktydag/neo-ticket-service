package com.neo.ticket.eventcatalog.domain;

import static com.neo.ticket.testsupport.TestFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neo.ticket.eventcatalog.domain.valueobject.EventSchedule;
import com.neo.ticket.shared.error.InvariantViolationException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EventSchedule")
class EventScheduleTest {

    @Test
    @DisplayName("given a start before an end, when created, then it reports its duration")
    void computesDuration() {
        EventSchedule schedule = EventSchedule.of(NOW, NOW.plusSeconds(3600));

        assertThat(schedule.duration()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("given an end before the start, when created, then it is rejected")
    void rejectsReversedDates() {
        assertThatThrownBy(() -> EventSchedule.of(NOW.plusSeconds(3600), NOW))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("endsAt must be after startsAt");
    }

    @Test
    @DisplayName("given identical instants, when created, then it is rejected")
    void rejectsZeroLengthEvents() {
        assertThatThrownBy(() -> EventSchedule.of(NOW, NOW))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    @DisplayName("given a duration beyond the maximum, when created, then it is rejected as a typo")
    void rejectsImplausiblyLongEvents() {
        assertThatThrownBy(() -> EventSchedule.of(NOW, NOW.plus(31, ChronoUnit.DAYS)))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("must not run for longer");
    }

    @Test
    @DisplayName("given a moment before the start, when asked, then it has not started")
    void comparesAgainstAMoment() {
        EventSchedule schedule = EventSchedule.of(NOW.plusSeconds(60), NOW.plusSeconds(3600));

        assertThat(schedule.startsAfter(NOW)).isTrue();
        assertThat(schedule.hasStartedBy(NOW)).isFalse();
        assertThat(schedule.hasStartedBy(NOW.plusSeconds(60))).isTrue();
        assertThat(schedule.startsAfter(NOW.plusSeconds(60))).isFalse();
    }

    @Test
    @DisplayName("given the same instants, when compared, then two schedules are equal")
    void comparesByValue() {
        assertThat(EventSchedule.of(NOW, NOW.plusSeconds(60)))
                .isEqualTo(EventSchedule.of(NOW, NOW.plusSeconds(60)))
                .hasSameHashCodeAs(EventSchedule.of(NOW, NOW.plusSeconds(60)));
    }
}
