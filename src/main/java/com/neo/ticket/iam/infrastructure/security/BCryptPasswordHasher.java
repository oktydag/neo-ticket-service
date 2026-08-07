package com.neo.ticket.iam.infrastructure.security;

import com.neo.ticket.iam.domain.PasswordHasher;
import com.neo.ticket.iam.domain.valueobject.PasswordHash;
import com.neo.ticket.iam.domain.valueobject.RawPassword;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    BCryptPasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PasswordHash hash(RawPassword rawPassword) {
        return PasswordHash.of(passwordEncoder.encode(rawPassword.value()));
    }

    @Override
    public boolean matches(RawPassword candidate, PasswordHash storedHash) {
        return passwordEncoder.matches(candidate.value(), storedHash.value());
    }
}
