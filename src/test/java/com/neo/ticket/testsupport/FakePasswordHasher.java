package com.neo.ticket.testsupport;

import com.neo.ticket.iam.domain.PasswordHasher;
import com.neo.ticket.iam.domain.valueobject.PasswordHash;
import com.neo.ticket.iam.domain.valueobject.RawPassword;

public class FakePasswordHasher implements PasswordHasher {

    private static final String PREFIX = "fake-bcrypt-hash-of::";

    private int matchCallCount;

    @Override
    public PasswordHash hash(RawPassword rawPassword) {
        return PasswordHash.of(PREFIX + rawPassword.value());
    }

    @Override
    public boolean matches(RawPassword candidate, PasswordHash storedHash) {
        matchCallCount++;
        return storedHash.value().equals(PREFIX + candidate.value());
    }

    public int matchCallCount() {
        return matchCallCount;
    }

    public void resetCallCount() {
        matchCallCount = 0;
    }
}
