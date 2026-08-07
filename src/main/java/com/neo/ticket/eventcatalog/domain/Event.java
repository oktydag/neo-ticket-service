package com.neo.ticket.eventcatalog.domain;

import com.neo.ticket.eventcatalog.domain.valueobject.Capacity;
import com.neo.ticket.eventcatalog.domain.valueobject.EventId;
import com.neo.ticket.eventcatalog.domain.valueobject.EventSchedule;
import com.neo.ticket.shared.domain.AggregateRoot;
import com.neo.ticket.shared.domain.Invariants;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.error.BusinessRuleViolationException;
import com.neo.ticket.shared.error.ErrorCode;
import com.neo.ticket.shared.error.ForbiddenOperationException;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event extends AggregateRoot<EventId> {

    public static final int TITLE_MIN_LENGTH = 3;
    public static final int TITLE_MAX_LENGTH = 140;
    public static final int VENUE_MIN_LENGTH = 3;
    public static final int VENUE_MAX_LENGTH = 200;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(name = "venue", nullable = false, length = VENUE_MAX_LENGTH)
    private String venue;

    @Embedded
    private EventSchedule schedule;

    @Embedded
    private Capacity capacity;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Event() {
    }

    private Event(EventId id, UserId ownerId, String title, String venue,
                  EventSchedule schedule, Capacity capacity, Instant now) {
        this.id = Invariants.requirePresent(id, "event.id").value();
        this.ownerId = Invariants.requirePresent(ownerId, "event.ownerId").value();
        this.title = Invariants.requireText(title, "title", TITLE_MIN_LENGTH, TITLE_MAX_LENGTH);
        this.venue = Invariants.requireText(venue, "venue", VENUE_MIN_LENGTH, VENUE_MAX_LENGTH);
        this.schedule = Invariants.requirePresent(schedule, "event.schedule");
        this.capacity = Invariants.requirePresent(capacity, "event.capacity");
        this.published = false;
        this.createdAt = Invariants.requirePresent(now, "event.createdAt");
        this.updatedAt = now;
    }

    public static Event createDraft(EventId id, UserId ownerId, String title, String venue,
                                    EventSchedule schedule, int capacity, Instant now) {
        requireStartsInFuture(schedule, now);
        Event event = new Event(id, ownerId, title, venue, schedule, Capacity.of(capacity), now);
        event.raise(new EventCreated(id, ownerId, now));
        return event;
    }

    public void update(Actor actor, String newTitle, String newVenue,
                       EventSchedule newSchedule, int newCapacity, Instant now) {
        assertModifiableBy(actor);
        Invariants.requirePresent(newSchedule, "event.schedule");

        if (published) {
            assertPublishedEditIsAllowed(newSchedule, newCapacity);
        } else {
            requireStartsInFuture(newSchedule, now);
            this.schedule = newSchedule;
        }

        this.title = Invariants.requireText(newTitle, "title", TITLE_MIN_LENGTH, TITLE_MAX_LENGTH);
        this.venue = Invariants.requireText(newVenue, "venue", VENUE_MIN_LENGTH, VENUE_MAX_LENGTH);
        this.capacity = capacity.resizeTo(newCapacity);
        this.updatedAt = Invariants.requirePresent(now, "updatedAt");
        raise(new EventUpdated(id(), ownerId(), now));
    }

    public void publish(Actor actor, Instant now) {
        assertModifiableBy(actor);
        if (published) {
            throw new BusinessRuleViolationException(ErrorCode.EVENT_ALREADY_PUBLISHED,
                    "Event %s is already published".formatted(id));
        }
        if (schedule.hasStartedBy(now)) {
            throw new BusinessRuleViolationException(ErrorCode.EVENT_NOT_RESERVABLE,
                    "Event %s cannot be published because it has already started".formatted(id));
        }
        this.published = true;
        this.publishedAt = now;
        this.updatedAt = now;
        raise(new EventPublished(id(), ownerId(), now));
    }

    public void reserveSeats(int seats, Instant now) {
        assertOpenForReservations(now);
        this.capacity = capacity.reserve(seats);
        this.updatedAt = now;
    }

    public void releaseSeats(int seats, Instant now) {
        this.capacity = capacity.release(seats);
        this.updatedAt = now;
    }

    public boolean isOpenForReservations(Instant now) {
        return published && schedule.startsAfter(now) && !capacity.isSoldOut();
    }

    public void assertModifiableBy(Actor actor) {
        Invariants.requirePresent(actor, "actor");
        if (!actor.mayAdminister(ownerId())) {
            throw new ForbiddenOperationException(ErrorCode.NOT_RESOURCE_OWNER,
                    "Event %s is owned by another organizer".formatted(id),
                    Map.of("eventId", id.toString()));
        }
    }

    public boolean isVisibleTo(Actor actor) {
        return published || (actor != null && actor.mayAdminister(ownerId()));
    }

    private void assertOpenForReservations(Instant now) {
        if (!published) {
            throw new BusinessRuleViolationException(ErrorCode.EVENT_NOT_PUBLISHED,
                    "Event %s is not published and cannot be reserved".formatted(id));
        }
        if (schedule.hasStartedBy(now)) {
            throw new BusinessRuleViolationException(ErrorCode.EVENT_NOT_RESERVABLE,
                    "Event %s has already started".formatted(id));
        }
    }

    private void assertPublishedEditIsAllowed(EventSchedule newSchedule, int newCapacity) {
        if (!schedule.equals(newSchedule)) {
            throw new BusinessRuleViolationException(ErrorCode.EVENT_IMMUTABLE_AFTER_PUBLISH,
                    "The schedule of a published event cannot be changed");
        }
        if (newCapacity < capacity.total()) {
            throw new BusinessRuleViolationException(ErrorCode.EVENT_IMMUTABLE_AFTER_PUBLISH,
                    "The capacity of a published event can only be increased",
                    Map.of("currentCapacity", capacity.total(), "requestedCapacity", newCapacity));
        }
    }

    private static void requireStartsInFuture(EventSchedule schedule, Instant now) {
        Invariants.requirePresent(schedule, "event.schedule");
        if (!schedule.startsAfter(now)) {
            throw new BusinessRuleViolationException(ErrorCode.EVENT_NOT_RESERVABLE,
                    "startsAt must be in the future");
        }
    }

    @Override
    public EventId id() {
        return new EventId(id);
    }

    public UserId ownerId() {
        return new UserId(ownerId);
    }

    public String title() {
        return title;
    }

    public String venue() {
        return venue;
    }

    public EventSchedule schedule() {
        return schedule;
    }

    public Capacity capacity() {
        return capacity;
    }

    public boolean isPublished() {
        return published;
    }

    public Instant publishedAt() {
        return publishedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }
}
