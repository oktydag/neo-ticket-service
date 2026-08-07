package com.neo.ticket.idempotency.domain;

import com.neo.ticket.shared.domain.valueobject.UserId;
import java.time.Instant;
import java.util.Optional;

public interface IdempotencyRecordRepository {

    Optional<IdempotencyRecord> find(String idempotencyKey, String endpoint, UserId userId);

    IdempotencyRecord save(IdempotencyRecord record);

    int deleteExpiredBefore(Instant cutoff);
}
