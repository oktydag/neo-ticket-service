package com.neo.ticket.iam.domain.valueobject;

import com.neo.ticket.shared.domain.Invariants;
import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {

    public static final int MAX_LENGTH = 254;

    private static final int MIN_LENGTH = 3;

    private static final Pattern SHAPE = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");

    public Email {
        value = Invariants.requireText(value, "email", MIN_LENGTH, MAX_LENGTH)
                .toLowerCase(Locale.ROOT);
        Invariants.require(SHAPE.matcher(value).matches(), "email is not a valid address");
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
