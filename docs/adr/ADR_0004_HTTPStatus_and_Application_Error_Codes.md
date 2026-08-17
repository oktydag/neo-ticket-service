# ADR 0004: HTTP Status and Application Error Codes

**Status:** Accepted

## Context

HTTP status codes only tell the general result of a request (success, not found, conflict, etc.).\
They are not enough to explain the exact business reason of an error.

For example, both of these can return `409 Conflict`:

- event capacity is full
- same idempotency key is used again

So I need an additional, stable error identifier.

## Decision

All API errors return `application/problem+json` (RFC 9457).

Each error response includes:

- HTTP status → general error type
- `code` → specific application error reason

Example:

```json
{
  "status": 409,
  "code": "EVENT_CAPACITY_EXCEEDED",
  "detail": "..."
}
```

### HTTP Status

Used for general categories:

- 400 → bad request
- 401 → not authenticated
- 403 → not allowed
- 404 → not found
- 409 → conflict
- 422 → validation/business rule error
- 429 → too many requests
- 500 → server error

### Error Code

Used for specific reasons:

- EVENT\_CAPACITY\_EXCEEDED
- IDEMPOTENCY\_KEY\_REUSED
- IDEMPOTENT\_RESULT\_UNAVAILABLE

Clients should rely on `code`, not on messages.

## Mapping

Errors flow like this:

```
Domain error → Exception → API handler → ProblemDetail → HTTP + code
```

The domain does not depend on HTTP.

## Consequences

### Positive

- Clear and stable error handling for clients
- HTTP and business logic are separated
- Same HTTP status can represent different errors
- Messages can change safely
- Easier debugging and logging

### Negative

- Need to maintain error codes
- Clients must understand codes for advanced handling

## Rejected Options

### Only HTTP status

Rejected because it is too generic and cannot describe specific business errors.

### Only error message

Rejected because messages are not stable and can change anytime.
