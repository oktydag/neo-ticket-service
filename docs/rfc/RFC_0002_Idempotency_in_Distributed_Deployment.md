# RFC 0002: Idempotency in Distributed Deployment

**Status:** Draft

## Context

The reservation API uses an `Idempotency-Key` and a database unique constraint to prevent duplicate reservations.

With a single application instance, this works as expected.

However, in a horizontally scaled deployment, the same idempotent request may reach different application instances:

```text
                Load Balancer
                 /        \
                ↓          ↓
          Instance A   Instance B
                \          /
                 ↓        ↓
                  PostgreSQL
```

The idempotency state must therefore be shared between instances.

## Proposal

Keep PostgreSQL as the source of truth for idempotency.

The unique constraint on:

```text
(idempotency_key, endpoint, user_id)
```

provides the concurrency guarantee regardless of which application instance receives the request.

```text
Instance A ─┐
Instance B ─┼──→ PostgreSQL
Instance C ─┘        │
                     ↓
              Unique Constraint
```

No instance-local memory or lock should be used for idempotency.

## Alternatives

### In-memory idempotency

Rejected because each instance would have its own state and application restarts would lose the records.

### Redis

Could provide shared idempotency state, but introduces another infrastructure dependency while PostgreSQL already provides the required consistency guarantee.

## Recommendation

Continue using **PostgreSQL as the shared idempotency store** when the application is horizontally scaled.

Introduce Redis or another distributed mechanism only if PostgreSQL becomes a measurable bottleneck for idempotency operations.

## Open Question

If traffic grows significantly, should idempotency reads/writes be optimized separately, or should the current PostgreSQL-based approach remain the source of truth?