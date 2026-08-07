package com.neo.ticket.iam.domain;

import com.neo.ticket.iam.domain.valueobject.PasswordHash;
import com.neo.ticket.iam.domain.valueobject.RawPassword;

public interface PasswordHasher {

    PasswordHash hash(RawPassword rawPassword);

    boolean matches(RawPassword candidate, PasswordHash storedHash);
}
