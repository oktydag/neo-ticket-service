package com.neo.ticket.iam.domain;

import com.neo.ticket.iam.domain.valueobject.RawPassword;
import com.neo.ticket.shared.error.InvariantViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RawPassword")
class RawPasswordTest {

    @Test
    @DisplayName("given a password at the minimum length, when created, then it is accepted")
    void acceptsTheShortestAllowedPassword() {
        String atLimit = "x".repeat(RawPassword.MIN_LENGTH);

        assertThat(RawPassword.of(atLimit).value()).isEqualTo(atLimit);
    }

    @Test
    @DisplayName("given a password one character short, when created, then it is rejected")
    void rejectsShortPasswords() {
        assertThatThrownBy(() -> RawPassword.of("x".repeat(RawPassword.MIN_LENGTH - 1)))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("at least");
    }

    @Test
    @DisplayName("given a password beyond BCrypt's input limit, when created, then it is rejected")
    void rejectsPasswordsBcryptWouldSilentlyTruncate() {
        assertThatThrownBy(() -> RawPassword.of("x".repeat(RawPassword.MAX_LENGTH + 1)))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("at most");
    }

    @Test
    @DisplayName("given no password, when created, then it is rejected")
    void rejectsNull() {
        assertThatThrownBy(() -> RawPassword.of(null)).isInstanceOf(InvariantViolationException.class);
    }

    @Test
    @DisplayName("given a password, when printed, then the value never appears")
    void keepsTheSecretOutOfLogs() {
        RawPassword password = RawPassword.of("super-secret-passphrase");

        assertThat(password.toString()).doesNotContain("super-secret-passphrase");
        assertThat(String.valueOf(password)).isEqualTo("RawPassword[***]");
    }
}
