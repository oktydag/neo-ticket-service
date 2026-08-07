package com.neo.ticket.idempotency.infrastructure.persistence;

import com.neo.ticket.idempotency.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface IdempotencyJpaRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByIdempotencyKeyAndEndpointAndUserId(
            String idempotencyKey, String endpoint, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from IdempotencyRecord r where r.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
