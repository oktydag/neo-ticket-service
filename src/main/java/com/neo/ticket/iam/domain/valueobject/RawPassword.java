package com.neo.ticket.iam.domain.valueobject;

import com.neo.ticket.shared.domain.Invariants;

public record RawPassword(String value) {

    public static final int MIN_LENGTH = 12;

    public static final int MAX_LENGTH = 72;

    private static final String REDACTED = "RawPassword[***]";

    public RawPassword {
        Invariants.requirePresent(value, "password");
        Invariants.require(value.length() >= MIN_LENGTH,
                "password must be at least %d characters".formatted(MIN_LENGTH));
        Invariants.require(value.length() <= MAX_LENGTH,
                "password must be at most %d characters".formatted(MAX_LENGTH));
        Invariants.require(!value.isBlank(), "password must not be blank");
    }

    public static RawPassword of(String value) {
        return new RawPassword(value);
    }

    @Override
    public String toString() {
        return REDACTED;
    }
}
