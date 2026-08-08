package com.neo.ticket.shared.ratelimit;

import com.neo.ticket.shared.error.InvariantViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenBucket")
class TokenBucketTest {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);
    private static final long START = 0L;

    private static long afterSeconds(long seconds) {
        return START + Duration.ofSeconds(seconds).toNanos();
    }

    @Test
    @DisplayName("given a fresh bucket, when requests arrive, then the full capacity is allowed at once")
    void allowsAFullBurst() {
        TokenBucket bucket = new TokenBucket(5, ONE_MINUTE, START);

        for (int i = 0; i < 5; i++) {
            assertThat(bucket.tryConsume(START)).as("request %d", i + 1).isTrue();
        }
    }

    @Test
    @DisplayName("given an exhausted bucket, when another request arrives, then it is refused")
    void refusesOnceExhausted() {
        TokenBucket bucket = new TokenBucket(2, ONE_MINUTE, START);
        bucket.tryConsume(START);
        bucket.tryConsume(START);

        assertThat(bucket.tryConsume(START)).isFalse();
        assertThat(bucket.availableTokens(START)).isZero();
    }

    @Test
    @DisplayName("given time has passed, when a request arrives, then one token has been earned back")
    void refillsOverTime() {
        TokenBucket bucket = new TokenBucket(6, ONE_MINUTE, START);
        for (int i = 0; i < 6; i++) {
            bucket.tryConsume(START);
        }

        assertThat(bucket.tryConsume(afterSeconds(9))).as("before a token is due").isFalse();
        assertThat(bucket.tryConsume(afterSeconds(10))).as("once a token is due").isTrue();
    }

    @Test
    @DisplayName("given a long idle period, when requests resume, then the bucket is capped at its capacity")
    void neverAccumulatesBeyondCapacity() {
        TokenBucket bucket = new TokenBucket(3, ONE_MINUTE, START);
        bucket.tryConsume(START);

        assertThat(bucket.availableTokens(afterSeconds(3600))).isEqualTo(3);
    }

    @Test
    @DisplayName("given partial progress towards a token, when time passes again, then the remainder is not lost")
    void carriesTheRemainderForward() {
        TokenBucket bucket = new TokenBucket(6, ONE_MINUTE, START);
        for (int i = 0; i < 6; i++) {
            bucket.tryConsume(START);
        }

        assertThat(bucket.tryConsume(afterSeconds(5))).isFalse();
        assertThat(bucket.tryConsume(afterSeconds(10))).isTrue();
    }

    @Test
    @DisplayName("given an exhausted bucket, when asked, then it reports how long to wait")
    void reportsHowLongToWait() {
        TokenBucket bucket = new TokenBucket(6, ONE_MINUTE, START);
        for (int i = 0; i < 6; i++) {
            bucket.tryConsume(START);
        }

        assertThat(bucket.timeUntilNextToken(START)).isEqualTo(Duration.ofSeconds(10));
        assertThat(bucket.timeUntilNextToken(afterSeconds(4))).isEqualTo(Duration.ofSeconds(6));
    }

    @Test
    @DisplayName("given tokens are available, when asked how long to wait, then the answer is not at all")
    void reportsNoWaitWhenTokensRemain() {
        TokenBucket bucket = new TokenBucket(5, ONE_MINUTE, START);

        assertThat(bucket.timeUntilNextToken(START)).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("given no requests for a long time, when checked, then the bucket counts as idle")
    void reportsItselfIdle() {
        TokenBucket bucket = new TokenBucket(5, ONE_MINUTE, START);

        assertThat(bucket.isIdleSince(afterSeconds(60), Duration.ofMinutes(15))).isFalse();
        assertThat(bucket.isIdleSince(afterSeconds(1200), Duration.ofMinutes(15))).isTrue();
    }

    @Test
    @DisplayName("given a nonsensical configuration, when constructed, then it is rejected")
    void rejectsInvalidConfiguration() {
        assertThatThrownBy(() -> new TokenBucket(0, ONE_MINUTE, START))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> new TokenBucket(5, Duration.ZERO, START))
                .isInstanceOf(InvariantViolationException.class);
    }
}
