package com.neo.ticket.shared.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimiter")
class RateLimiterTest {

    private static final RateLimitProperties.Policy POLICY =
            new RateLimitProperties.Policy(3, Duration.ofMinutes(1));

    private static final String AUTH = "authentication";
    private static final String GENERAL = "general";

    private AtomicLong now;
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        now = new AtomicLong(0);
        rateLimiter = new RateLimiter(now::get);
    }

    private void advance(Duration duration) {
        now.addAndGet(duration.toNanos());
    }

    @Test
    @DisplayName("given requests within the limit, when checked, then each is allowed with a falling quota")
    void allowsUpToTheLimit() {
        assertThat(rateLimiter.check(AUTH, "10.0.0.1", POLICY).remainingQuota()).isEqualTo(2);
        assertThat(rateLimiter.check(AUTH, "10.0.0.1", POLICY).remainingQuota()).isEqualTo(1);

        RateLimiter.Decision third = rateLimiter.check(AUTH, "10.0.0.1", POLICY);
        assertThat(third.allowed()).isTrue();
        assertThat(third.remainingQuota()).isZero();
    }

    @Test
    @DisplayName("given the limit is exceeded, when checked, then it is refused with a retry hint")
    void refusesBeyondTheLimit() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.check(AUTH, "10.0.0.1", POLICY);
        }

        RateLimiter.Decision decision = rateLimiter.check(AUTH, "10.0.0.1", POLICY);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.retryAfter()).isPositive();
    }

    @Test
    @DisplayName("given one client is throttled, when another calls, then it is unaffected")
    void isolatesClientsFromEachOther() {
        for (int i = 0; i < 4; i++) {
            rateLimiter.check(AUTH, "10.0.0.1", POLICY);
        }

        assertThat(rateLimiter.check(AUTH, "10.0.0.2", POLICY).allowed()).isTrue();
    }

    @Test
    @DisplayName("given one policy is exhausted, when the same client uses another, then it is unaffected")
    void keepsPoliciesSeparate() {
        for (int i = 0; i < 4; i++) {
            rateLimiter.check(AUTH, "10.0.0.1", POLICY);
        }

        assertThat(rateLimiter.check(GENERAL, "10.0.0.1", POLICY).allowed()).isTrue();
    }

    @Test
    @DisplayName("given the refill period passes, when the client returns, then it is allowed again")
    void recoversAfterTheWindow() {
        for (int i = 0; i < 4; i++) {
            rateLimiter.check(AUTH, "10.0.0.1", POLICY);
        }

        advance(Duration.ofMinutes(1));

        assertThat(rateLimiter.check(AUTH, "10.0.0.1", POLICY).allowed()).isTrue();
    }

    @Test
    @DisplayName("given idle clients, when eviction runs, then their buckets are dropped")
    void evictsIdleBuckets() {
        rateLimiter.check(AUTH, "10.0.0.1", POLICY);
        rateLimiter.check(AUTH, "10.0.0.2", POLICY);
        assertThat(rateLimiter.trackedBucketCount()).isEqualTo(2);

        advance(Duration.ofHours(1));
        int evicted = rateLimiter.evictIdleBuckets();

        assertThat(evicted).isEqualTo(2);
        assertThat(rateLimiter.trackedBucketCount()).isZero();
    }

    @Test
    @DisplayName("given an active client, when eviction runs, then its bucket survives")
    void keepsActiveBuckets() {
        rateLimiter.check(AUTH, "10.0.0.1", POLICY);

        advance(Duration.ofMinutes(1));
        rateLimiter.evictIdleBuckets();

        assertThat(rateLimiter.trackedBucketCount()).isEqualTo(1);
    }
}
