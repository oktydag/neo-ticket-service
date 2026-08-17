# ADR 0006: Audit Logging via Domain Events

**Status:** Accepted

## Context

The service needs to answer, after the fact, **who performed an important action and what happened**.

Audit logging should not become part of the core business logic or force domain/application code to depend directly on the audit persistence mechanism.

The service already uses domain events to communicate that an important domain action has occurred.

## Decision

Important state-changing operations publish a **domain event** after the relevant domain action occurs.

The audit mechanism listens to these events and creates an `AuditLog` record containing information such as:

- actor (`Actor`)
- action/event type
- affected aggregate/entity
- relevant metadata
- timestamp

Conceptually:

```text id="c8s2qk"
Command
   ↓
Application Handler
   ↓
Domain operation
   ↓
Domain Event
   │
   └──────────────→ Audit Handler
                         ↓
                     AuditLog
                         ↓
                    PostgreSQL
```

The domain does not directly create or persist `AuditLog` records. It only expresses that a relevant business event occurred.

Audit logging is therefore a **consumer of domain events**, rather than a responsibility of the domain itself.

## Consequences

### Positive

- Audit concerns remain separated from business logic.
- Domain code does not depend on the audit persistence model.
- New audit requirements can be added by handling existing events.
- Audit records contain the actor and action needed for traceability.
- The approach remains suitable for the current modular monolith without introducing Kafka or another messaging infrastructure.

### Negative

- Additional event and handler code is required.
- The audit flow must be considered when defining domain events.
- If audit persistence is performed asynchronously in the future, delivery and consistency guarantees will need to be revisited.