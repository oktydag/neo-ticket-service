package com.neo.ticket.iam.domain.valueobject;

import com.neo.ticket.shared.domain.Invariants;

public record PasswordHash(String value) {

    public static final int MAX_LENGTH = 100;

    private static final int MIN_LENGTH = 20;

    private static final String REDACTED = "PasswordHash[***]";

    public PasswordHash {
        Invariants.requireText(value, "passwordHash", MIN_LENGTH, MAX_LENGTH);
    }

    public static PasswordHash of(String value) {
        return new PasswordHash(value);
    }

    @Override
    public String toString() {
        return REDACTED;
    }
}
