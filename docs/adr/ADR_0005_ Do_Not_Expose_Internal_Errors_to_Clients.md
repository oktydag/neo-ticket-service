# ADR 0006: Do Not Expose Internal Errors to Clients

**Status:** Accepted

## Context

Unexpected errors can contain sensitive or implementation-specific information such as:

- stack traces
- database errors
- SQL/JPA details
- internal class or package names
- infrastructure details

These details are useful for debugging but should not become part of the public API contract.

## Decision

Unexpected internal exceptions are handled centrally at the API boundary.

The client receives a generic `500 Internal Server Error` using the same `application/problem+json` error format used by other API errors.

For example:

```json
{
  "status": 500,
  "code": "INTERNAL_ERROR",
  "title": "Internal Server Error",
  "detail": "An unexpected error occurred."
}
```

The original exception is **not exposed to the client**.

Instead, the full exception and relevant context are logged server-side for troubleshooting.

Expected business/application errors continue to be mapped to their specific HTTP status and application error code.

```text
Expected error
    ↓
Known application code
    ↓
ProblemDetail → Client

Unexpected exception
    ↓
Log full exception
    ↓
INTERNAL_ERROR
    ↓
ProblemDetail → Client
```

## Consequences

### Positive

- Internal implementation details are not exposed.
- API error responses remain stable.
- Sensitive database and infrastructure information is protected.
- Developers still have the full exception available through server-side logs.
- Unexpected failures are clearly distinguished from known business errors.

### Negative

- The client receives less information about unexpected failures.
- Effective server-side logging and monitoring are required for troubleshooting.
