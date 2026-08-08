package com.neo.ticket.shared.ratelimit;

import com.neo.ticket.shared.domain.Invariants;
import java.time.Duration;

public final class TokenBucket {

    private final long capacity;
    private final long nanosPerToken;

    private long availableTokens;
    private long lastRefillNanos;

    public TokenBucket(long capacity, Duration refillPeriod, long createdAtNanos) {
        Invariants.require(capacity > 0, "rate limit capacity must be positive");
        Invariants.requirePresent(refillPeriod, "refillPeriod");
        Invariants.require(!refillPeriod.isZero() && !refillPeriod.isNegative(),
                "rate limit refill period must be positive");
        this.capacity = capacity;
        this.nanosPerToken = Math.max(1L, refillPeriod.toNanos() / capacity);
        this.availableTokens = capacity;
        this.lastRefillNanos = createdAtNanos;
    }

    public synchronized boolean tryConsume(long nowNanos) {
        refill(nowNanos);
        if (availableTokens <= 0) {
            return false;
        }
        availableTokens--;
        return true;
    }

    public synchronized Duration timeUntilNextToken(long nowNanos) {
        refill(nowNanos);
        if (availableTokens > 0) {
            return Duration.ZERO;
        }
        long elapsedSinceRefill = nowNanos - lastRefillNanos;
        return Duration.ofNanos(Math.max(0, nanosPerToken - elapsedSinceRefill));
    }

    public synchronized boolean isIdleSince(long nowNanos, Duration idleThreshold) {
        return nowNanos - lastRefillNanos > idleThreshold.toNanos();
    }

    private void refill(long nowNanos) {
        long elapsed = nowNanos - lastRefillNanos;
        if (elapsed < nanosPerToken) {
            return;
        }
        long earned = elapsed / nanosPerToken;
        availableTokens = Math.min(capacity, availableTokens + earned);
        lastRefillNanos += earned * nanosPerToken;
    }

    public synchronized long availableTokens(long nowNanos) {
        refill(nowNanos);
        return availableTokens;
    }
}
