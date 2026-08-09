# ADR 0008: Application Monitoring and Health Checks

**Status:** Accepted

## Context

The service needs basic operational visibility to detect whether the application is running correctly and to observe its behavior in production.

Monitoring should cover both:

- application availability
- runtime/application metrics

## Decision

I used **Spring Boot Actuator** for application health and **Micrometer with Prometheus** for metrics.

Health endpoints are used by Kubernetes probes:

```text
/actuator/health/liveness
/actuator/health/readiness
```

- **Liveness** indicates whether the application is alive.
- **Readiness** indicates whether the application is ready to receive traffic.

Application metrics are exposed through Micrometer/Prometheus and can be used to monitor request behavior, JVM/runtime metrics and other application-level measurements.

The application does not introduce a separate monitoring service or custom monitoring infrastructure.

## Flow

```text
Application
    │
    ├── Actuator Health
    │       ├── Liveness
    │       └── Readiness
    │               ↓
    │          Kubernetes
    │
    └── Micrometer
            ↓
        Prometheus
            ↓
       Monitoring
```

## Consequences

### Positive

- Standard Spring Boot monitoring approach.
- Kubernetes can automatically detect unhealthy or unready instances.
- Runtime and application metrics are available without custom infrastructure.
- Health and metrics concerns remain separate from business logic.

### Negative

- Metrics storage and visualization require Prometheus/monitoring infrastructure.
- Exposed health and metrics endpoints must be appropriately secured in production.