# RFC 0004: Pending Reservation Expiry

**Status:** Draft

## Context

A reservation holds seats as soon as it is created. `Reservation.place()` moves the seats out of the event's available capacity immediately, before the customer has confirmed anything:

```text
POST /reservations
       ↓
Reservation PENDING
       ↓
Event capacity: reserved_seats +N
```

`ReservationStatus.holdsSeats()` returns true for every status except `CANCELLED`, so a `PENDING` reservation keeps those seats.

The only ways out of `PENDING` are an explicit `confirm` or `cancel` call:

```text
PENDING ──confirm──→ CONFIRMED
   │
   └────cancel────→ CANCELLED  (seats released)
```

There is no time limit. If the customer never calls either endpoint, the seats stay held **forever**.

This means a popular event can show as sold out while none of those seats were actually paid for or confirmed. In the worst case a client can hold the entire capacity of an event by creating reservations and simply walking away.

The service already has a place where this kind of clean-up happens. `HousekeepingScheduler` runs on a fixed delay and already removes expired idempotency records and refresh tokens — but it does not touch reservations.

## Proposal

Give a `PENDING` reservation a **hold deadline**. When the deadline passes without a confirmation, the reservation is expired automatically and its seats return to the event.

```text
PENDING ──confirm───────→ CONFIRMED
   │
   ├──cancel────────────→ CANCELLED   (seats released)
   │
   └──deadline passes───→ EXPIRED     (seats released)
```

Only `PENDING` reservations expire. Once a reservation is `CONFIRMED` it is no longer time-limited.

## Alternatives

### Do nothing

Keep the current behaviour and rely on customers to cancel what they do not want.

Simple, but capacity stays locked by abandoned reservations and the seat count shown to other customers becomes misleading.

### Scheduled expiry sweep

A scheduled job periodically finds `PENDING` reservations past their deadline and releases them.

```text
Every N minutes
       ↓
Find PENDING where deadline < now
       ↓
Release seats + mark EXPIRED
```

Uses the mechanism the service already has (`HousekeepingScheduler`), and the released seats become visible to other customers without anyone having to read that reservation first.

The trade-off is that seats are freed up to one sweep interval late, so the interval has to be short enough to be acceptable.

### Lazy expiry on read

Instead of a job, treat an overdue reservation as expired whenever it is next read.

No scheduler involved, but a reservation nobody reads is never expired — which is exactly the abandoned case this RFC is about. The seats would stay held indefinitely.

## Recommendation

Adopt the **scheduled expiry sweep**, reusing `HousekeepingScheduler` rather than introducing new infrastructure.

Suggested shape:

- Store a deadline on the reservation when it is placed.
- Make the hold duration a configuration property (like the existing `neo.idempotency.ttl`) instead of hard-coding it.
- Treat expiry as a normal domain transition on the aggregate, so the same rules that release seats on cancellation are reused rather than duplicated.
- Raise a domain event for the expiry so the audit trail records it like any other state change.

Note that this job runs on every instance today, so expiry inherits the same multi-instance scheduling question already raised for housekeeping. The sweep must be safe to run concurrently.

## Open Questions

1. **How long should the hold last?** Short holds free capacity quickly but may expire a reservation while the customer is still completing the flow. This likely depends on whether a payment step is introduced later.

2. **Should the hold duration be global or per-event?** A single service-wide value is simpler, but a high-demand event may justify a shorter hold than a quiet one.

3. **Is `EXPIRED` a separate status, or just `CANCELLED`?** Reusing `CANCELLED` requires no schema or enum change and already releases the seats correctly. A separate `EXPIRED` status makes it possible to tell "the customer gave up" apart from "the system reclaimed the seats" in reporting and in the audit trail.
