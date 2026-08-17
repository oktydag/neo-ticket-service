# ADR 0002: Using Optimistic and Pessimistic Locking

**Status:** Accepted

## Context

The service has concurrent operations that modify shared state. The most critical example is event capacity during reservation.

Without concurrency control, two requests can read the same available capacity and both successfully create a reservation, resulting in **overselling**.

Different operations have different contention characteristics, so a single locking strategy is not appropriate for every use case.

## Decision

I use **pessimistic locking** for operations where concurrent access to the same resource can directly violate a critical business invariant.

For example, when creating a reservation:

```text
BEGIN
  SELECT Event FOR UPDATE
  ↓
  Check available capacity
  ↓
  Create reservation
  ↓
  Update capacity
COMMIT
```

`PESSIMISTIC_WRITE` ensures that concurrent transactions cannot simultaneously modify the same event capacity and therefore prevents overselling.

I use **optimistic locking** for operations where conflicts are expected to be less frequent and holding a database lock for the duration of the transaction is unnecessary.

These entities use a version field:

```text
Entity
 ├── state
 └── version
```

## Why Both?

The strategies solve different concurrency problems:

| Strategy         | When                                   | Reason                           |
| ---------------- | -------------------------------------- | -------------------------------- |
| Pessimistic Lock | High contention / critical invariant   | Serialize concurrent access      |
| Optimistic Lock  | Lower contention / independent updates | Avoid unnecessary database locks |

For **reservation and event capacity**, correctness is more important than allowing concurrent writes, so pessimistic locking is preferred.

For other state updates, optimistic locking provides conflict detection with less locking overhead.

## Consequences

### Positive

- Prevents overselling under concurrent reservations.
- Avoids unnecessary pessimistic locks for low-contention operations.
- Makes concurrency behavior explicit per use case.
- Preserves domain invariants at the persistence/transaction boundary.

### Negative

- Pessimistic locking can reduce throughput under high contention.
- Long-running transactions can increase lock contention.
- Optimistic locking requires conflicts to be handled appropriately.
- I must choose the strategy based on the business operation rather than applying one strategy globally.

## Rejected Alternative

### Use only Optimistic Locking

Rejected for the reservation flow because high contention around event capacity could cause many concurrent conflicts and requires retry/conflict handling. The business invariant is better protected by serializing access to the capacity row.

### Use only Pessimistic Locking

Rejected because not every update requires serialization. Applying pessimistic locking globally would unnecessarily increase database contention and reduce concurrency.
