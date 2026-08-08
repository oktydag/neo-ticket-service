package com.neo.ticket.api;

import com.neo.ticket.testsupport.ApiClient;
import com.neo.ticket.testsupport.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@TestPropertySource(properties = {
        "neo.rate-limit.enabled=true",
        "neo.rate-limit.authentication.capacity=3",
        "neo.rate-limit.authentication.refill-period=60s",
        "neo.rate-limit.general.capacity=500",
        "neo.rate-limit.general.refill-period=60s",
        "neo.client-address.trust-forwarded-headers=true"
})
@DisplayName("Rate limiting")
class RateLimitIntegrationTest {

    private static final int AUTH_CAPACITY = 3;
    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private static final AtomicInteger ADDRESS_SEQUENCE = new AtomicInteger();

    @LocalServerPort
    private int port;

    private ApiClient api;
    private Map<String, String> clientAddress;

    @BeforeEach
    void setUp() {
        api = new ApiClient(port);
        clientAddress = Map.of(FORWARDED_FOR, "203.0.113." + ADDRESS_SEQUENCE.incrementAndGet());
    }

    private ApiClient.Response attemptLogin() {
        return api.post("/api/auth/login", null,
                Map.of("email", "nobody@neo.io", "password", "definitely-the-wrong-one"),
                clientAddress);
    }

    @Test
    @DisplayName("given repeated sign-in attempts, when the allowance runs out, "
            + "then further attempts are refused")
    void throttlesRepeatedSignInAttempts() {
        List<HttpStatus> statuses = new ArrayList<>();
        for (int i = 0; i < AUTH_CAPACITY + 3; i++) {
            statuses.add(attemptLogin().status());
        }

        assertThat(statuses.subList(0, AUTH_CAPACITY))
                .as("the allowance is spent on real attempts, which get a real rejection")
                .containsOnly(HttpStatus.UNAUTHORIZED);
        assertThat(statuses.subList(AUTH_CAPACITY, statuses.size()))
                .as("and everything past the allowance is throttled")
                .containsOnly(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("given the limit is hit, when refused, then the response says how long to wait")
    void tellsTheCallerWhenToComeBack() {
        for (int i = 0; i < AUTH_CAPACITY; i++) {
            attemptLogin();
        }

        ApiClient.Response throttled = attemptLogin();

        assertThat(throttled.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(throttled.body().get("errorCode")).isEqualTo("RATE_LIMIT_EXCEEDED");
        assertThat(throttled.contentType()).contains("application/problem+json");
        assertThat(throttled.header(HttpHeaders.RETRY_AFTER)).isNotBlank();
        assertThat(throttled.header("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(throttled.header("X-RateLimit-Limit")).isEqualTo(String.valueOf(AUTH_CAPACITY));
    }

    @Test
    @DisplayName("given one client is throttled, when a different client calls, then it is unaffected")
    void throttlesPerClientRatherThanGlobally() {
        for (int i = 0; i < AUTH_CAPACITY + 1; i++) {
            attemptLogin();
        }

        ApiClient.Response fromElsewhere = api.post("/api/auth/login", null,
                Map.of("email", "nobody@neo.io", "password", "definitely-the-wrong-one"),
                Map.of(FORWARDED_FOR, "198.51.100.7"));

        assertThat(fromElsewhere.status())
                .as("one noisy client must not lock everyone else out")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("given the auth allowance is spent, when the health check runs, then it still answers")
    void neverThrottlesTheHealthCheck() {
        for (int i = 0; i < AUTH_CAPACITY + 2; i++) {
            attemptLogin();
        }

        assertThat(api.get("/actuator/health", null, clientAddress).status())
                .as("a throttled client must not take monitoring down with it")
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("given the auth allowance is spent, when a non-auth endpoint is called, "
            + "then its own allowance applies")
    void keepsTheAuthAllowanceSeparate() {
        for (int i = 0; i < AUTH_CAPACITY + 2; i++) {
            attemptLogin();
        }

        assertThat(api.get("/api/events/public", null, clientAddress).status())
                .isEqualTo(HttpStatus.OK);
    }
}
