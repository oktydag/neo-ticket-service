# RFC 0001: Reliable Domain Events

**Status:** Draft

## Context

The application uses domain events for important business actions such as reservations.

Currently, these events are handled inside the application. This is sufficient as long as event consumers remain within the same application.

However, if events are later sent to external systems such as Kafka, a reliability problem appears:

```text
Database Commit ✓
       ↓
Event Publish ✗
```

The business operation succeeds, but the event is lost.


The opposite can also happen:

```text
Event publish ✓
     ↓
DB ROLLBACK ✗
```

A consumer may receive an event describing a state that does not exist.

This creates a **dual-write consistency problem**.

## Proposal

Use the **Transactional Outbox Pattern** when domain events need to be reliably delivered outside the application.

Instead of publishing the event directly:

```text
Business Operation
       ↓
Database
       ↓
Publish Event
```

store the event in an `outbox` table in the same database transaction:

```text
Business Operation
       ↓
Database Transaction
   ├── Business Data
   └── Outbox Event
       ↓
     Commit
       ↓
 Event Publisher
       ↓
 External System
```

This guarantees that the event is stored whenever the business operation is successfully committed.

## Alternatives

### Keep current in-process events

Simple and sufficient while events are only consumed inside the application.

### Transactional Outbox

Provides reliable event delivery and allows failed publications to be retried, but introduces an additional table and publisher process.

### Kafka directly

Provides a scalable event infrastructure but introduces unnecessary infrastructure and operational complexity for the current system.

## Recommendation

**Do not introduce Outbox yet.**

Keep the current in-process domain events while all consumers remain inside the application.

Adopt Transactional Outbox when an event needs to be delivered to an external system or reliable asynchronous delivery becomes a requirement.

```text
Business Transaction
        │
        ├── Domain State
        │
        └── Outbox Event
                │
              COMMIT
                │
                ▼
          Event Publisher
                │
                ▼
             Kafka
                │
        ┌───────┼────────┐
        ▼       ▼        ▼
      Audit  Notification Analytics
```

## Open Question

At what point does the need for reliable external event delivery justify introducing the Outbox Pattern?