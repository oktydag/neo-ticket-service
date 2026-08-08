package com.neo.ticket.reservation;

import com.neo.ticket.eventcatalog.application.EventView;
import com.neo.ticket.eventcatalog.application.command.CreateEventCommand;
import com.neo.ticket.eventcatalog.application.command.handlers.CreateEventHandler;
import com.neo.ticket.eventcatalog.application.command.handlers.PublishEventHandler;
import com.neo.ticket.eventcatalog.domain.Event;
import com.neo.ticket.eventcatalog.domain.EventRepository;
import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.iam.domain.User;
import com.neo.ticket.reservation.application.ReservationView;
import com.neo.ticket.reservation.application.command.handlers.CancelReservationHandler;
import com.neo.ticket.reservation.application.command.handlers.ReserveSeatsHandler;
import com.neo.ticket.reservation.domain.ReservationRepository;
import com.neo.ticket.reservation.domain.valueobject.ReservationId;
import com.neo.ticket.reservation.domain.valueobject.ReservationStatus;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.error.DomainException;
import com.neo.ticket.shared.error.ErrorCode;
import com.neo.ticket.testsupport.IntegrationTest;
import com.neo.ticket.testsupport.TestAccounts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@DisplayName("Concurrent reservations")
class OverbookingConcurrencyTest {

    private static final int TIMEOUT_SECONDS = 60;

    @Autowired
    private ReserveSeatsHandler reserveSeatsHandler;

    @Autowired
    private CancelReservationHandler cancelReservationHandler;

    @Autowired
    private CreateEventHandler createEventHandler;

    @Autowired
    private PublishEventHandler publishEventHandler;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private TestAccounts accounts;

    @Autowired
    private Clock clock;

    private record Fixture(EventId eventId, List<Actor> customers) {
    }

    private Fixture publishedEventWith(int capacity, int customerCount) {
        User organizerUser = accounts.create(Role.ORGANIZER);
        Actor organizer = organizerUser.toActor();
        Instant startsAt = clock.instant().plus(30, ChronoUnit.DAYS);

        EventView created = createEventHandler.handle(organizer, new CreateEventCommand(
                "Concurrency Conference", "Neo Arena, Hall B",
                startsAt, startsAt.plusSeconds(7200), capacity));
        publishEventHandler.handle(organizer, new EventId(created.id()));

        List<Actor> customers = new ArrayList<>();
        for (int i = 0; i < customerCount; i++) {
            customers.add(accounts.create(Role.CUSTOMER).toActor());
        }
        return new Fixture(new EventId(created.id()), customers);
    }

    private <T> List<Future<T>> runSimultaneously(List<Callable<T>> tasks) throws InterruptedException {
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    startGun.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    return task.call();
                }));
            }
            startGun.countDown();
        }
        return futures;
    }

    @Test
    @DisplayName("given more buyers than seats, when they all reserve at once, "
            + "then exactly the capacity is sold")
    void neverSellsMoreSeatsThanExist() throws Exception {
        int capacity = 25;
        int contenders = 80;
        Fixture fixture = publishedEventWith(capacity, contenders);

        AtomicInteger sold = new AtomicInteger();
        List<ErrorCode> rejections = java.util.Collections.synchronizedList(new ArrayList<>());

        List<Callable<Void>> attempts = fixture.customers().stream()
                .map(customer -> (Callable<Void>) () -> {
                    try {
                        reserveSeatsHandler.handle(customer, fixture.eventId(), 1);
                        sold.incrementAndGet();
                    } catch (DomainException rejected) {
                        rejections.add(rejected.errorCode());
                    }
                    return null;
                })
                .toList();

        for (Future<Void> future : runSimultaneously(attempts)) {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        assertThat(sold.get())
                .as("exactly the available seats are sold, no more and none wasted")
                .isEqualTo(capacity);
        assertThat(rejections)
                .as("every loser is told why")
                .hasSize(contenders - capacity)
                .containsOnly(ErrorCode.INSUFFICIENT_CAPACITY);

        Event event = eventRepository.findById(fixture.eventId()).orElseThrow();
        assertThat(event.capacity().reserved()).isEqualTo(capacity);
        assertThat(event.capacity().remaining()).isZero();
        assertThat(reservationRepository.totalSeatsHeldFor(fixture.eventId()))
                .as("the count on the event agrees with the reservations that justify it")
                .isEqualTo(capacity);
    }

    @Test
    @DisplayName("given buyers asking for several seats each, when they collide, "
            + "then the totals still balance")
    void handlesMultiSeatReservationsWithoutDrift() throws Exception {
        int capacity = 30;
        int contenders = 40;
        int seatsEach = 3;
        Fixture fixture = publishedEventWith(capacity, contenders);

        AtomicInteger seatsSold = new AtomicInteger();

        List<Callable<Void>> attempts = fixture.customers().stream()
                .map(customer -> (Callable<Void>) () -> {
                    try {
                        reserveSeatsHandler.handle(customer, fixture.eventId(), seatsEach);
                        seatsSold.addAndGet(seatsEach);
                    } catch (DomainException ignored) {
                    }
                    return null;
                })
                .toList();

        for (Future<Void> future : runSimultaneously(attempts)) {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        Event event = eventRepository.findById(fixture.eventId()).orElseThrow();
        assertThat(seatsSold.get()).isEqualTo(capacity);
        assertThat(event.capacity().reserved()).isEqualTo(capacity);
        assertThat(reservationRepository.totalSeatsHeldFor(fixture.eventId())).isEqualTo(capacity);
    }

    @Test
    @DisplayName("given one reservation, when many threads cancel it at once, "
            + "then its seats are returned exactly once")
    void cancellingTwiceAtOnceReleasesSeatsOnlyOnce() throws Exception {
        int capacity = 10;
        int seats = 4;
        Fixture fixture = publishedEventWith(capacity, 1);
        Actor customer = fixture.customers().getFirst();

        ReservationView reservation = reserveSeatsHandler.handle(customer, fixture.eventId(), seats);
        ReservationId reservationId = new ReservationId(reservation.id());

        AtomicInteger cancelled = new AtomicInteger();
        List<Callable<Void>> attempts = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            attempts.add(() -> {
                try {
                    cancelReservationHandler.handle(customer, reservationId);
                    cancelled.incrementAndGet();
                } catch (RuntimeException expectedForAllButOne) {
                }
                return null;
            });
        }

        for (Future<Void> future : runSimultaneously(attempts)) {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        assertThat(cancelled.get()).as("only one cancellation takes effect").isEqualTo(1);

        Event event = eventRepository.findById(fixture.eventId()).orElseThrow();
        assertThat(event.capacity().reserved())
                .as("the seats came back once, not once per attempt")
                .isZero();
        assertThat(reservationRepository.findById(reservationId).orElseThrow().status())
                .isEqualTo(ReservationStatus.CANCELLED);
    }
}
