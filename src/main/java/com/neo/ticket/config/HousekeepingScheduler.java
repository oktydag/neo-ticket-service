package com.neo.ticket.config;

import com.neo.ticket.iam.domain.RefreshTokenRepository;
import com.neo.ticket.idempotency.domain.IdempotencyRecordRepository;
import com.neo.ticket.shared.ratelimit.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Component
@EnableScheduling
class HousekeepingScheduler {

    private static final Logger log = LoggerFactory.getLogger(HousekeepingScheduler.class);

    private static final long HOURLY = 60 * 60 * 1000L;
    private static final long QUARTER_HOURLY = 15 * 60 * 1000L;
    private static final long STARTUP_DELAY = 60 * 1000L;

    private final IdempotencyRecordRepository idempotencyRecords;
    private final RefreshTokenRepository refreshTokens;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    HousekeepingScheduler(IdempotencyRecordRepository idempotencyRecords,
                          RefreshTokenRepository refreshTokens,
                          RateLimiter rateLimiter,
                          Clock clock) {
        this.idempotencyRecords = idempotencyRecords;
        this.refreshTokens = refreshTokens;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = HOURLY, initialDelay = STARTUP_DELAY)
    @Transactional
    void purgeExpiredRecords() {
        int idempotencyRemoved = idempotencyRecords.deleteExpiredBefore(clock.instant());
        int tokensRemoved = refreshTokens.deleteExpiredBefore(clock.instant());
        if (idempotencyRemoved > 0 || tokensRemoved > 0) {
            log.info("Housekeeping removed {} idempotency record(s) and {} refresh token(s)",
                    idempotencyRemoved, tokensRemoved);
        }
    }

    @Scheduled(fixedDelay = QUARTER_HOURLY, initialDelay = STARTUP_DELAY)
    void purgeIdleRateLimitBuckets() {
        rateLimiter.evictIdleBuckets();
    }
}
