package com.neo.ticket.idempotency.application;

public record IdempotentOutcome<T>(T value, int httpStatus, boolean replayed) {
}
