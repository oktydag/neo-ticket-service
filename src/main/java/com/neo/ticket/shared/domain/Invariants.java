package com.neo.ticket.shared.domain;

import com.neo.ticket.shared.error.InvariantViolationException;

public final class Invariants {

    private Invariants() {
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new InvariantViolationException(message);
        }
    }

    public static <T> T requirePresent(T value, String field) {
        if (value == null) {
            throw new InvariantViolationException("%s must be provided".formatted(field));
        }
        return value;
    }

    public static String requireText(String value, String field, int minLength, int maxLength) {
        requirePresent(value, field);
        String trimmed = value.trim();
        require(!trimmed.isEmpty(), "%s must not be blank".formatted(field));
        require(trimmed.length() >= minLength,
                "%s must be at least %d characters".formatted(field, minLength));
        require(trimmed.length() <= maxLength,
                "%s must be at most %d characters".formatted(field, maxLength));
        return trimmed;
    }

    public static int requireRange(int value, String field, int minInclusive, int maxInclusive) {
        require(value >= minInclusive, "%s must be at least %d".formatted(field, minInclusive));
        require(value <= maxInclusive, "%s must be at most %d".formatted(field, maxInclusive));
        return value;
    }
}
