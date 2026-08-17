# ADR 0001: Using DDD and Lightweight CQRS

**Status:** Accepted

## Context

The service manages event capacity and reservations where correctness of business rules and concurrent access are critical.

The main domain concepts are:

- `Event` / `Capacity`
- `Reservation`
- `User`
- `IdempotencyRecord`
- `AuditLog`

The application also has different responsibilities for **commands** (state-changing operations) and **queries** (read operations).

I considered whether Domain-Driven Design (DDD) and CQRS were necessary or whether a simpler layered architecture would be sufficient.

## Decision

I use **DDD principles** to keep business rules inside the domain rather than spreading them across controllers, services, and persistence code.

The application is organized around bounded contexts:

```text
shared
 ├── iam
 ├── eventcatalog
 ├── reservation
 ├── idempotency
 └── audit
```

The bounded contexts use the same PostgreSQL database but own different tables and domain responsibilities. They are **logical boundaries**, not separate databases or microservices.

The main request flow is:

```text
API
 ↓
Application
 ↓
Domain
 ↓
Repository
 ↓
PostgreSQL
```

Within the Application layer, I use **lightweight / Level-1 CQRS**:

```text
Command → Command Handler → Domain → Repository
Query   → Query Handler   → Repository → Read DTO
```

Commands and queries therefore have separate handlers and responsibilities, but they still run inside the same application and use the same database.

I deliberately do **not** introduce separate read/write databases, event sourcing, or a distributed CQRS architecture.

## Why DDD?

DDD is useful here because the important part of the service is not CRUD itself but enforcing domain invariants such as:

- event capacity cannot be exceeded,
- reservations must belong to valid events,
- domain state transitions must remain consistent,
- concurrent reservations must not oversell capacity.

Keeping these rules in the domain makes them easier to test and prevents the API or persistence layer from becoming the owner of business logic.

DDD is therefore used as a **design tool for modelling the domain**, not as an attempt to introduce every DDD pattern.

## Why Level-1 CQRS?

The system has naturally different write and read use cases.

Commands change state and require domain/business rules:

```text
CreateReservation
CancelReservation
CreateEvent
```

Queries primarily retrieve data:

```text
GetEvent
ListEvents
GetReservation
```

Separating them at the application layer improves clarity and keeps command handlers focused on state changes while query handlers can optimize read models independently.

However, introducing separate databases, messaging, event sourcing, or asynchronous projections would add operational complexity that is not justified by the current requirements.

Therefore, CQRS is limited to **separating command and query responsibilities at the application layer**.

## Consequences

### Positive

- Business rules remain inside the domain.
- Bounded contexts provide clear ownership of business concepts.
- Command and query responsibilities are explicit.
- Concurrency-sensitive operations are easier to reason about and test.
- The architecture can evolve toward more advanced CQRS if future requirements justify it.

### Negative

- More classes and packages than a simple CRUD architecture.
- Some abstractions may be unnecessary for very simple use cases.
- The team must understand the boundaries between API, Application, Domain, and Repository layers.
- Level-1 CQRS does not provide the scalability benefits of a distributed CQRS architecture.

## Rejected Alternatives

### Simple Layered CRUD

Rejected because business rules such as reservation capacity and concurrency would tend to leak into application services and persistence code.

### Full CQRS / Event Sourcing

Rejected because separate read/write stores, asynchronous projections, and event sourcing would introduce significant operational and consistency complexity without a current business requirement.

### Microservices per Bounded Context

Rejected because the current domain boundaries are logical boundaries within a modular monolith. Separate services would introduce network communication, distributed transactions, deployment, and operational overhead that is not currently justified.