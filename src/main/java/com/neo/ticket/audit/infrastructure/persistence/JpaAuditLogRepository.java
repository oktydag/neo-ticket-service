package com.neo.ticket.audit.infrastructure.persistence;

import com.neo.ticket.audit.domain.AuditLog;
import com.neo.ticket.audit.domain.AuditLogRepository;
import org.springframework.stereotype.Repository;

@Repository
class JpaAuditLogRepository implements AuditLogRepository {

    private final AuditLogJpaRepository jpaRepository;

    JpaAuditLogRepository(AuditLogJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        return jpaRepository.save(auditLog);
    }
}
