package com.neo.ticket.audit.domain;

public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);
}
