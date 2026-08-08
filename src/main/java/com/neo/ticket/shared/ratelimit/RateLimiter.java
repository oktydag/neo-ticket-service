package com.neo.ticket.shared.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private static final Duration IDLE_EVICTION_THRESHOLD = Duration.ofMinutes(15);

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final LongSupplier nanoTimeSource;

    public RateLimiter() {
        this(System::nanoTime);
    }

    public RateLimiter(LongSupplier nanoTimeSource) {
        this.nanoTimeSource = nanoTimeSource;
    }

    public Decision check(String policyName, String clientKey, RateLimitProperties.Policy policy) {
        long now = nanoTimeSource.getAsLong();
        TokenBucket bucket = buckets.computeIfAbsent(
                policyName + '|' + clientKey,
                ignored -> new TokenBucket(policy.capacity(), policy.refillPeriod(), now));

        if (bucket.tryConsume(now)) {
            return new Decision(true, Duration.ZERO, bucket.availableTokens(now));
        }
        return new Decision(false, bucket.timeUntilNextToken(now), 0);
    }

    public int evictIdleBuckets() {
        long now = nanoTimeSource.getAsLong();
        int sizeBefore = buckets.size();
        buckets.values().removeIf(bucket -> bucket.isIdleSince(now, IDLE_EVICTION_THRESHOLD));
        int evicted = sizeBefore - buckets.size();
        if (evicted > 0) {
            log.debug("Evicted {} idle rate-limit bucket(s); {} remain", evicted, buckets.size());
        }
        return evicted;
    }

    public int trackedBucketCount() {
        return buckets.size();
    }

    public record Decision(boolean allowed, Duration retryAfter, long remainingQuota) {
    }
}
