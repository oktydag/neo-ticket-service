package com.neo.ticket.audit.application;

import com.neo.ticket.audit.domain.AuditLog;
import com.neo.ticket.audit.domain.AuditLogRepository;
import com.neo.ticket.shared.domain.AuditableEvent;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.security.CurrentActorProvider;
import com.neo.ticket.shared.web.RequestMetadataHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Component
public class AuditTrailListener {

    private static final Logger log = LoggerFactory.getLogger(AuditTrailListener.class);

    private final AuditLogRepository auditLogRepository;
    private final CurrentActorProvider currentActorProvider;
    private final Clock clock;

    public AuditTrailListener(AuditLogRepository auditLogRepository,
                              CurrentActorProvider currentActorProvider,
                              Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.currentActorProvider = currentActorProvider;
        this.clock = clock;
    }
    
    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void on(AuditableEvent event) {
        try {
            auditLogRepository.save(AuditLog.record(
                    resolveActorId(event),
                    event.action(),
                    event.resourceType(),
                    event.resourceId(),
                    RequestMetadataHolder.current(),
                    clock.instant()));
        } catch (RuntimeException failure) {
            log.error("Failed to write audit record for action {} on {} {}",
                    event.action(), event.resourceType(), event.resourceId(), failure);
        }
    }

    private UserId resolveActorId(AuditableEvent event) {
        return event.actorId()
                .or(() -> currentActorProvider.find().map(Actor::userId))
                .orElse(null);
    }
}
