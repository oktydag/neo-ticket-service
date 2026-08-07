package com.neo.ticket.idempotency.infrastructure.persistence;

import com.neo.ticket.idempotency.domain.IdempotencyRecord;
import com.neo.ticket.idempotency.domain.IdempotencyRecordRepository;
import com.neo.ticket.shared.domain.valueobject.UserId;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
class JpaIdempotencyRecordRepository implements IdempotencyRecordRepository {

    private final IdempotencyJpaRepository jpaRepository;

    JpaIdempotencyRecordRepository(IdempotencyJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<IdempotencyRecord> find(String idempotencyKey, String endpoint, UserId userId) {
        return jpaRepository.findByIdempotencyKeyAndEndpointAndUserId(
                idempotencyKey, endpoint, userId.value());
    }

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        return jpaRepository.saveAndFlush(record);
    }

    @Override
    public int deleteExpiredBefore(Instant cutoff) {
        return jpaRepository.deleteExpiredBefore(cutoff);
    }
}
