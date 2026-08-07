package com.neo.ticket.shared.web;

public final class IdempotencyHeaders {

    public static final String HEADER = "Idempotency-Key";

    public static final String REPLAYED_HEADER = "Idempotency-Replayed";

    public static final int MIN_KEY_LENGTH = 8;
    public static final int MAX_KEY_LENGTH = 128;

    private IdempotencyHeaders() {
    }
}
