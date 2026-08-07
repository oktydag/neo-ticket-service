package com.neo.ticket.idempotency.application;

public sealed interface IdempotencyDecision {

    record Proceed() implements IdempotencyDecision {
    }

    record Replay(int httpStatus, String serialisedBody) implements IdempotencyDecision {
    }

    record InProgress() implements IdempotencyDecision {
    }

    record PayloadMismatch() implements IdempotencyDecision {
    }

    record ResultUnavailable() implements IdempotencyDecision {
    }
}
