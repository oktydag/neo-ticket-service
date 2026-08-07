package com.neo.ticket.idempotency.application;

import com.neo.ticket.idempotency.domain.IdempotencyRecord;
import com.neo.ticket.idempotency.domain.IdempotencyRecordRepository;
import com.neo.ticket.idempotency.domain.valueobject.IdempotencyStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class IdempotencyRecordStore {

    private final IdempotencyRecordRepository repository;

    public IdempotencyRecordStore(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertClaim(IdempotencyContext context, String requestHash, Instant now, Duration ttl) {
        repository.save(IdempotencyRecord.claim(
                context.key(), context.endpoint(), context.userId(), requestHash, now, ttl));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyDecision inspect(IdempotencyContext context, String requestHash,
                                       Instant now, Duration ttl) {
        Optional<IdempotencyRecord> found =
                repository.find(context.key(), context.endpoint(), context.userId());
        if (found.isEmpty()) {
            return new IdempotencyDecision.Proceed();
        }

        IdempotencyRecord existing = found.get();
        if (existing.isExpiredAt(now) || existing.status() == IdempotencyStatus.FAILED) {
            existing.restart(requestHash, now, ttl);
            repository.save(existing);
            return new IdempotencyDecision.Proceed();
        }
        if (!existing.matchesRequest(requestHash)) {
            return new IdempotencyDecision.PayloadMismatch();
        }
        return switch (existing.status()) {
            case COMPLETED -> existing.isReplayable()
                    ? new IdempotencyDecision.Replay(existing.responseStatus(), existing.responseBody())
                    : new IdempotencyDecision.ResultUnavailable();
            case IN_PROGRESS -> new IdempotencyDecision.InProgress();
            case FAILED -> new IdempotencyDecision.Proceed();
        };
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(IdempotencyContext context, String serialisedResponse, int httpStatus) {
        repository.find(context.key(), context.endpoint(), context.userId())
                .ifPresent(record -> {
                    record.complete(serialisedResponse, httpStatus);
                    repository.save(record);
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(IdempotencyContext context) {
        repository.find(context.key(), context.endpoint(), context.userId())
                .ifPresent(record -> {
                    record.fail();
                    repository.save(record);
                });
    }
}
