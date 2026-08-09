# RFC 0003: Rate Limiting Strategy

**Status:** Draft

## Context

The application has rate limiting for protecting API endpoints from excessive requests.

Currently, rate limiting is handled at the application level. This works correctly for a single application instance, but horizontal scaling introduces a question:

```text
              Load Balancer
              /           \
             ↓             ↓
        Instance A     Instance B
          limit            limit
```

Each instance having its own counter means the effective limit can increase with the number of instances.

## Proposal

Define the rate limiting scope and decide whether the limit should be **instance-local** or **shared across instances**.

The main options are:

### Application-local

Each instance maintains its own rate limit.

```text
Instance A → Counter A
Instance B → Counter B
```

Simple and requires no additional infrastructure, but the effective global limit changes as instances scale.

### Distributed

All instances share the same rate-limit state.

```text
Instance A ─┐
Instance B ─┼──→ Shared Store
Instance C ─┘
```

This provides a consistent global limit but requires a shared store such as Redis.

## Recommendation

Keep the current application-level rate limiting while the service runs with a small number of instances and the limit does not need to be globally strict.

Introduce distributed rate limiting when:

- the application is horizontally scaled,
- a strict global limit is required, or
- application-local limits become insufficient.

Redis should only be introduced when this requirement is justified.

## Open Question

Should rate limits be defined per application instance or as a global limit across all instances?