package com.neo.ticket.iam.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neo.ticket.iam.domain.valueobject.Email;
import com.neo.ticket.shared.error.InvariantViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Email")
class EmailTest {

    @Test
    @DisplayName("given mixed case and padding, when created, then it is normalised")
    void normalisesForComparison() {
        assertThat(Email.of("  Ada.Lovelace@Neo.IO  ").value()).isEqualTo("ada.lovelace@neo.io");
    }

    @Test
    @DisplayName("given two addresses differing only in case, when compared, then they are equal")
    void treatsCaseVariantsAsTheSameAccount() {
        assertThat(Email.of("ADA@NEO.IO")).isEqualTo(Email.of("ada@neo.io"));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"ada@neo.io", "ada.lovelace+tag@sub.neo.co.uk", "a@b.cd"})
    @DisplayName("given a plausible address, when created, then it is accepted")
    void acceptsPlausibleAddresses(String candidate) {
        assertThat(Email.of(candidate).value()).isEqualTo(candidate);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"no-at-sign", "@neo.io", "ada@", "ada@neo", "ada @neo.io", "ada@@neo.io"})
    @DisplayName("given a malformed address, when created, then it is rejected")
    void rejectsMalformedAddresses(String candidate) {
        assertThatThrownBy(() -> Email.of(candidate)).isInstanceOf(InvariantViolationException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("given nothing at all, when created, then it is rejected")
    void rejectsMissingValues(String candidate) {
        assertThatThrownBy(() -> Email.of(candidate)).isInstanceOf(InvariantViolationException.class);
    }

    @Test
    @DisplayName("given an address longer than the RFC limit, when created, then it is rejected")
    void rejectsOverlongAddresses() {
        String tooLong = "a".repeat(Email.MAX_LENGTH) + "@neo.io";

        assertThatThrownBy(() -> Email.of(tooLong)).isInstanceOf(InvariantViolationException.class);
    }
}
