package com.neo.ticket.audit.domain;

import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.error.InvariantViolationException;
import com.neo.ticket.shared.web.RequestMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuditLog")
class AuditLogTest {

    private static final Instant NOW = Instant.parse("2026-06-01T10:00:00Z");
    private static final UserId ACTOR = UserId.of("11111111-1111-1111-1111-111111111111");
    private static final RequestMetadata METADATA = new RequestMetadata("10.0.0.1", "curl/8", "req-123");

    @Nested
    @DisplayName("recording")
    class Recording {

        @Test
        @DisplayName("given valid inputs, when recorded, then every field is populated")
        void populatesEveryField() {
            AuditLog log = AuditLog.record(ACTOR, "user.registered", "user", "42", METADATA, NOW);

            assertThat(log.id()).isNotNull();
            assertThat(log.actorId()).isEqualTo(ACTOR);
            assertThat(log.action()).isEqualTo("user.registered");
            assertThat(log.resourceType()).isEqualTo("user");
            assertThat(log.resourceId()).isEqualTo("42");
            assertThat(log.ip()).isEqualTo("10.0.0.1");
            assertThat(log.userAgent()).isEqualTo("curl/8");
            assertThat(log.requestId()).isEqualTo("req-123");
            assertThat(log.createdAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("given no actor, when recorded, then the entry is anonymous")
        void allowsAnonymousActors() {
            AuditLog log = AuditLog.record(null, "user.login", "user", "42", METADATA, NOW);

            assertThat(log.actorId()).isNull();
        }

        @Test
        @DisplayName("given two records with identical content, when compared, then their ids differ")
        void mintsAFreshIdentifier() {
            AuditLog first = AuditLog.record(ACTOR, "a", "r", "1", METADATA, NOW);
            AuditLog second = AuditLog.record(ACTOR, "a", "r", "1", METADATA, NOW);

            assertThat(first.id()).isNotEqualTo(second.id());
        }

        @Test
        @DisplayName("given padded text, when recorded, then the stored value is trimmed")
        void trimsTextFields() {
            AuditLog log = AuditLog.record(ACTOR, "  user.registered  ", "  user  ", "  42  ", METADATA, NOW);

            assertThat(log.action()).isEqualTo("user.registered");
            assertThat(log.resourceType()).isEqualTo("user");
            assertThat(log.resourceId()).isEqualTo("42");
        }
    }

    @Nested
    @DisplayName("required fields")
    class RequiredFields {

        @ParameterizedTest(name = "action = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("given a blank action, when recorded, then it is rejected")
        void rejectsBlankAction(String action) {
            assertThatThrownBy(() -> AuditLog.record(ACTOR, action, "user", "42", METADATA, NOW))
                    .isInstanceOf(InvariantViolationException.class)
                    .hasMessageContaining("action");
        }

        @ParameterizedTest(name = "resourceType = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("given a blank resource type, when recorded, then it is rejected")
        void rejectsBlankResourceType(String resourceType) {
            assertThatThrownBy(() -> AuditLog.record(ACTOR, "a", resourceType, "42", METADATA, NOW))
                    .isInstanceOf(InvariantViolationException.class)
                    .hasMessageContaining("resourceType");
        }

        @ParameterizedTest(name = "resourceId = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("given a blank resource id, when recorded, then it is rejected")
        void rejectsBlankResourceId(String resourceId) {
            assertThatThrownBy(() -> AuditLog.record(ACTOR, "a", "r", resourceId, METADATA, NOW))
                    .isInstanceOf(InvariantViolationException.class)
                    .hasMessageContaining("resourceId");
        }

        @Test
        @DisplayName("given no timestamp, when recorded, then it is rejected")
        void rejectsMissingTimestamp() {
            assertThatThrownBy(() -> AuditLog.record(ACTOR, "a", "r", "1", METADATA, null))
                    .isInstanceOf(InvariantViolationException.class)
                    .hasMessageContaining("createdAt");
        }
    }

    @Nested
    @DisplayName("length limits")
    class LengthLimits {

        @Test
        @DisplayName("given an action beyond the max, when recorded, then it is rejected")
        void rejectsActionOverMax() {
            String tooLong = "a".repeat(AuditLog.MAX_ACTION_LENGTH + 1);

            assertThatThrownBy(() -> AuditLog.record(ACTOR, tooLong, "r", "1", METADATA, NOW))
                    .isInstanceOf(InvariantViolationException.class)
                    .hasMessageContaining("action");
        }

        @Test
        @DisplayName("given a resource type beyond the max, when recorded, then it is rejected")
        void rejectsResourceTypeOverMax() {
            String tooLong = "a".repeat(AuditLog.MAX_RESOURCE_TYPE_LENGTH + 1);

            assertThatThrownBy(() -> AuditLog.record(ACTOR, "a", tooLong, "1", METADATA, NOW))
                    .isInstanceOf(InvariantViolationException.class)
                    .hasMessageContaining("resourceType");
        }

        @Test
        @DisplayName("given a resource id beyond the max, when recorded, then it is rejected")
        void rejectsResourceIdOverMax() {
            String tooLong = "a".repeat(AuditLog.MAX_RESOURCE_ID_LENGTH + 1);

            assertThatThrownBy(() -> AuditLog.record(ACTOR, "a", "r", tooLong, METADATA, NOW))
                    .isInstanceOf(InvariantViolationException.class)
                    .hasMessageContaining("resourceId");
        }
    }

    @Nested
    @DisplayName("metadata handling")
    class MetadataHandling {

        @Test
        @DisplayName("given an IP longer than the column, when recorded, then it is truncated")
        void truncatesLongIp() {
            RequestMetadata metadata = new RequestMetadata("a".repeat(AuditLog.MAX_IP_LENGTH + 50), "ua", "rid");

            AuditLog log = AuditLog.record(ACTOR, "a", "r", "1", metadata, NOW);

            assertThat(log.ip()).hasSize(AuditLog.MAX_IP_LENGTH);
        }

        @Test
        @DisplayName("given a user agent longer than the column, when recorded, then it is truncated")
        void truncatesLongUserAgent() {
            String tooLong = "u".repeat(RequestMetadata.MAX_USER_AGENT_LENGTH + 50);
            RequestMetadata metadata = new RequestMetadata("ip", tooLong, "rid");

            AuditLog log = AuditLog.record(ACTOR, "a", "r", "1", metadata, NOW);

            assertThat(log.userAgent()).hasSize(RequestMetadata.MAX_USER_AGENT_LENGTH);
        }

        @Test
        @DisplayName("given a request id longer than the column, when recorded, then it is truncated to 64")
        void truncatesLongRequestId() {
            RequestMetadata metadata = new RequestMetadata("ip", "ua", "r".repeat(120));

            AuditLog log = AuditLog.record(ACTOR, "a", "r", "1", metadata, NOW);

            assertThat(log.requestId()).hasSize(64);
        }

        @Test
        @DisplayName("given blank metadata, when recorded, then unknown is stored instead")
        void substitutesUnknownForBlankMetadata() {
            RequestMetadata metadata = new RequestMetadata("", "  ", null);

            AuditLog log = AuditLog.record(ACTOR, "a", "r", "1", metadata, NOW);

            assertThat(log.ip()).isEqualTo(RequestMetadata.UNKNOWN);
            assertThat(log.userAgent()).isEqualTo(RequestMetadata.UNKNOWN);
            assertThat(log.requestId()).isEqualTo(RequestMetadata.UNKNOWN);
        }
    }
}
