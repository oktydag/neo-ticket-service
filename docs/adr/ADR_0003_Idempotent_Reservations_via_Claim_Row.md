# ADR 0002: Idempotent Reservations via a Claim Row

**Status:** Accepted

## Context

A client that times out while waiting for a reservation response cannot know whether the reservation was created. It may retry the same request.

Without idempotency, the retry could create a second reservation.

The service must guarantee that requests using the same `Idempotency-Key` execute the reservation **at most once**, including when requests arrive concurrently.

## Options Considered

### Check then execute

Rejected because two concurrent requests can both pass the check before either one creates the reservation.

### Distributed lock

A Redis-based lock could solve the concurrency problem, but would introduce an additional infrastructure dependency and consistency/failure boundary.

### Claim row with a unique constraint

Chosen because PostgreSQL already provides the required atomicity and mutual exclusion through a unique constraint.

## Decision

I use an **idempotency claim row** as the concurrency boundary.

The `Idempotency-Key` is stored in `idempotency_keys` with a unique constraint on:

```text
(idempotency_key, endpoint, user_id)
```

## The flow is:

```text
POST /reservations
        │
        │ Idempotency-Key
        ▼
Check IdempotencyRecord
        │
   ┌────┴───────────────┐
   │                    │
  Exists             Not exists
   │                    │
   ▼                    ▼
Return existing     Create claim
result              + reservation
                         │
                         ▼
                     Commit


                     
Claim → Execute → Record
```


1. **Claim** — I insert the idempotency record. The request that wins the unique constraint owns the operation. A concurrent duplicate is treated as an expected duplicate, not as an application error.
2. **Execute** — I create the reservation.
3. **Record** — I store the response so subsequent retries can return the original result without executing the operation again.

The claim is committed before the reservation transaction starts, ensuring concurrent requests can see it. Failed operations are marked as `FAILED`, allowing the client to retry.

The request body is stored as a **SHA-256 fingerprint**. Reusing the same key with a different request body is rejected with `409 IDEMPOTENCY_KEY_REUSED`.

The idempotency scope includes the **user and endpoint**, preventing keys from colliding across users or operations.

The idempotency guard is applied at the controller/application boundary rather than a servlet filter, keeping the behavior explicit and avoiding response buffering and transaction-boundary problems.

## Consequences

### Positive

- Concurrent retries cannot create duplicate reservations.
- No additional infrastructure such as Redis is required.
- The database unique constraint provides the concurrency guarantee.
- Successful responses can be replayed without executing the operation again.
- Idempotency is verified with concurrency tests.

### Negative

- The idempotency table grows with traffic and requires TTL-based cleanup.
- Large responses may not be stored; such retries return `IDEMPOTENT_RESULT_UNAVAILABLE` rather than executing the operation again.
- A reused key with a different request body is rejected.

## Verification

`IdempotencyConcurrencyTest` verifies the concurrency guarantee by sending **16 concurrent requests with the same idempotency key** and asserting that exactly one reservation is created.
