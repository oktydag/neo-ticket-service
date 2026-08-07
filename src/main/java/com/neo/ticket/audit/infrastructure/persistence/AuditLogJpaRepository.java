package com.neo.ticket.audit.infrastructure.persistence;

import com.neo.ticket.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AuditLogJpaRepository extends JpaRepository<AuditLog, UUID> {
}
