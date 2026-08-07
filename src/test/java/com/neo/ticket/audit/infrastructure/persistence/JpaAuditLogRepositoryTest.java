package com.neo.ticket.audit.infrastructure.persistence;

import com.neo.ticket.audit.domain.AuditLog;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.web.RequestMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("JpaAuditLogRepository")
class JpaAuditLogRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-06-01T10:00:00Z");
    private static final UserId ACTOR = UserId.of("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("given an audit log, when saved, then it is forwarded to the JPA repository")
    void delegatesToTheJpaRepository() {
        AuditLogJpaRepository jpa = mock(AuditLogJpaRepository.class);
        JpaAuditLogRepository repository = new JpaAuditLogRepository(jpa);
        AuditLog log = AuditLog.record(ACTOR, "a.b", "resource", "res-1",
                RequestMetadata.unknown(), NOW);
        when(jpa.save(log)).thenReturn(log);

        AuditLog saved = repository.save(log);

        assertThat(saved).isSameAs(log);
        verify(jpa).save(same(log));
        verifyNoMoreInteractions(jpa);
    }
}
