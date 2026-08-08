package com.neo.ticket.idempotency;

import com.neo.ticket.eventcatalog.application.EventView;
import com.neo.ticket.eventcatalog.application.command.CreateEventCommand;
import com.neo.ticket.eventcatalog.application.command.handlers.CreateEventHandler;
import com.neo.ticket.eventcatalog.application.command.handlers.PublishEventHandler;
import com.neo.ticket.eventcatalog.domain.Event;
import com.neo.ticket.eventcatalog.domain.EventRepository;
import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.idempotency.application.IdempotencyContext;
import com.neo.ticket.idempotency.application.IdempotencyGuard;
import com.neo.ticket.idempotency.application.IdempotentOutcome;
import com.neo.ticket.reservation.application.ReservationView;
import com.neo.ticket.reservation.application.command.handlers.ReserveSeatsHandler;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.testsupport.IntegrationTest;
import com.neo.ticket.testsupport.TestAccounts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@DisplayName("Concurrent idempotent requests")
class IdempotencyConcurrencyTest {

    private static final int TIMEOUT_SECONDS = 60;
    private static final String ENDPOINT = "POST /api/events/{id}/reservations";

    @Autowired
    private IdempotencyGuard idempotencyGuard;

    @Autowired
    private ReserveSeatsHandler reserveSeatsHandler;

    @Autowired
    private CreateEventHandler createEventHandler;

    @Autowired
    private PublishEventHandler publishEventHandler;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TestAccounts accounts;

    @Autowired
    private Clock clock;

    private EventId publishedEvent(Actor organizer, int capacity) {
        Instant startsAt = clock.instant().plus(30, ChronoUnit.DAYS);
        EventView created = createEventHandler.handle(organizer, new CreateEventCommand(
                "Idempotency Conference", "Neo Arena, Hall B",
                startsAt, startsAt.plusSeconds(7200), capacity));
        publishEventHandler.handle(organizer, new EventId(created.id()));
        return new EventId(created.id());
    }

    @Test
    @DisplayName("given the same key sent by many threads at once, when they race, "
            + "then exactly one reservation is created")
    void oneKeyProducesOneReservation() throws Exception {
        Actor organizer = accounts.create(Role.ORGANIZER).toActor();
        Actor customer = accounts.create(Role.CUSTOMER).toActor();
        EventId eventId = publishedEvent(organizer, 50);

        IdempotencyContext context = new IdempotencyContext(
                UUID.randomUUID().toString(), ENDPOINT + eventId, customer.userId());
        Map<String, Object> requestPayload = Map.of("seats", 2);

        List<String> reservationIds = Collections.synchronizedList(new ArrayList<>());
        List<String> rejections = Collections.synchronizedList(new ArrayList<>());

        List<Callable<Void>> attempts = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            attempts.add(() -> {
                try {
                    IdempotentOutcome<ReservationView> outcome = idempotencyGuard.execute(
                            context, requestPayload, HttpStatus.CREATED.value(), ReservationView.class,
                            () -> reserveSeatsHandler.handle(customer, eventId, 2));
                    reservationIds.add(outcome.value().id().toString());
                } catch (RuntimeException rejected) {
                    rejections.add(rejected.getClass().getSimpleName());
                }
                return null;
            });
        }

        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Callable<Void> attempt : attempts) {
                futures.add(pool.submit(() -> {
                    startGun.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    return attempt.call();
                }));
            }
            startGun.countDown();
        }
        for (Future<Void> future : futures) {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        assertThat(reservationIds)
                .as("every thread that got an answer got the same reservation")
                .isNotEmpty();
        assertThat(Set.copyOf(reservationIds))
                .as("only one reservation was ever created")
                .hasSize(1);
        assertThat(reservationIds.size() + rejections.size()).isEqualTo(16);

        Event event = eventRepository.findById(eventId).orElseThrow();
        assertThat(event.capacity().reserved())
                .as("seats were taken exactly once despite 16 identical requests")
                .isEqualTo(2);
    }
}
