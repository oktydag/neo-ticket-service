package com.neo.ticket.api;

import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.web.IdempotencyHeaders;
import com.neo.ticket.testsupport.ApiClient;
import com.neo.ticket.testsupport.IntegrationTest;
import com.neo.ticket.testsupport.TestAccounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@DisplayName("API")
class ApiFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestAccounts accounts;

    private ApiClient api;

    @BeforeEach
    void setUp() {
        api = new ApiClient(port);
    }

    private Instant futureStart() {
        return Instant.now().plus(30, ChronoUnit.DAYS);
    }

    private Map<String, Object> eventBody(int capacity) {
        Instant startsAt = futureStart();
        return Map.of(
                "title", "Spring Boot 4 Deep Dive",
                "venue", "Neo Arena, Hall B",
                "startsAt", startsAt.toString(),
                "endsAt", startsAt.plusSeconds(7200).toString(),
                "capacity", capacity);
    }

    private String publishedEvent(String organizerToken, int capacity) {
        ApiClient.Response created = api.post("/api/events", organizerToken, eventBody(capacity));
        String eventId = (String) created.body().get("id");
        api.post("/api/events/%s/publish".formatted(eventId), organizerToken, null);
        return eventId;
    }

    @Nested
    @DisplayName("authentication")
    class Authentication {

        @Test
        @DisplayName("given a new account, when registering then logging in, then tokens are issued")
        void registerThenLogIn() {
            String email = "flow-%s@neo.io".formatted(UUID.randomUUID());

            ApiClient.Response registered = api.post("/api/auth/register", null,
                    Map.of("email", email, "password", "a-long-enough-password"));
            assertThat(registered.status()).isEqualTo(HttpStatus.CREATED);

            ApiClient.Response loggedIn = api.post("/api/auth/login", null,
                    Map.of("email", email, "password", "a-long-enough-password"));
            assertThat(loggedIn.status()).isEqualTo(HttpStatus.OK);
            assertThat(loggedIn.body()).containsKeys("accessToken", "refreshToken", "expiresIn");
            assertThat(loggedIn.body().get("tokenType")).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("given no token, when calling a protected endpoint, then a problem response is returned")
        void protectedEndpointsRequireAToken() {
            ApiClient.Response response = api.get("/api/events", null);

            assertThat(response.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.body().get("errorCode")).isEqualTo("AUTHENTICATION_REQUIRED");
            assertThat(response.contentType()).contains("application/problem+json");
        }

        @Test
        @DisplayName("given a forged token, when calling a protected endpoint, then it is rejected")
        void rejectsForgedTokens() {
            String forged = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjpbIkFETUlOIl19.not-a-signature";

            assertThat(api.get("/api/events", forged).status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("given any response, when it returns, then it carries a request id")
        void everyResponseIsCorrelatable() {
            ApiClient.Response response = api.get("/api/events/public", null);

            assertThat(response.header(com.neo.ticket.shared.web.RequestMetadata.REQUEST_ID_HEADER))
                    .isNotBlank();
        }
    }

    @Nested
    @DisplayName("role-based access")
    class RoleBasedAccess {

        @Test
        @DisplayName("given a customer, when creating an event, then it is forbidden")
        void customersCannotCreateEvents() {
            String token = api.tokenFor(accounts.create(Role.CUSTOMER));

            ApiClient.Response response = api.post("/api/events", token, eventBody(10));

            assertThat(response.status()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.body().get("errorCode")).isEqualTo("ACCESS_DENIED");
        }

        @Test
        @DisplayName("given an organizer, when creating an event, then it is allowed")
        void organizersCanCreateEvents() {
            String token = api.tokenFor(accounts.create(Role.ORGANIZER));

            assertThat(api.post("/api/events", token, eventBody(10)).status())
                    .isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("given an organizer, when editing another organizer's event, then it is forbidden")
        void organizersCannotTouchEachOthersEvents() {
            String ownerToken = api.tokenFor(accounts.create(Role.ORGANIZER));
            String intruderToken = api.tokenFor(accounts.create(Role.ORGANIZER));
            String eventId = (String) api.post("/api/events", ownerToken, eventBody(10)).body().get("id");

            ApiClient.Response response = api.put("/api/events/" + eventId, intruderToken, eventBody(10));

            assertThat(response.status()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.body().get("errorCode")).isEqualTo("NOT_RESOURCE_OWNER");
        }

        @Test
        @DisplayName("given an admin, when editing another owner's event, then it is allowed")
        void adminsMayActOnAnyEvent() {
            String ownerToken = api.tokenFor(accounts.create(Role.ORGANIZER));
            String adminToken = api.tokenFor(accounts.create(Role.ADMIN));
            String eventId = (String) api.post("/api/events", ownerToken, eventBody(10)).body().get("id");

            assertThat(api.post("/api/events/%s/publish".formatted(eventId), adminToken, null).status())
                    .isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("given a non-admin, when reading actuator internals, then it is forbidden")
        void actuatorInternalsAreStaffOnly() {
            String customerToken = api.tokenFor(accounts.create(Role.CUSTOMER));
            String adminToken = api.tokenFor(accounts.create(Role.ADMIN));

            assertThat(api.get("/actuator/metrics", customerToken).status()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(api.get("/actuator/metrics", adminToken).status()).isEqualTo(HttpStatus.OK);
            assertThat(api.get("/actuator/health", null).status())
                    .as("liveness must stay public")
                    .isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("discovery")
    class Discovery {

        @Test
        @DisplayName("given a draft and a published event, when browsing anonymously, "
                + "then only the published one appears")
        void showsOnlyPublishedEvents() {
            String organizerToken = api.tokenFor(accounts.create(Role.ORGANIZER));
            api.post("/api/events", organizerToken, eventBody(10));
            String publishedId = publishedEvent(organizerToken, 10);

            ApiClient.Response response = api.get("/api/events/public?q=Deep+Dive", null);

            assertThat(response.status()).isEqualTo(HttpStatus.OK);
            assertThat(response.list("content"))
                    .extracting(row -> row.get("id"))
                    .contains(publishedId);
            assertThat(response.list("content"))
                    .allSatisfy(row -> assertThat(row.get("published")).isEqualTo(true));
        }

        @Test
        @DisplayName("given an oversized page request, when browsing, then the page size is capped")
        void capsThePageSize() {
            ApiClient.Response response = api.get("/api/events/public?size=100000", null);

            assertThat(response.body().get("size")).isEqualTo(
                    com.neo.ticket.shared.web.PageResponse.MAX_PAGE_SIZE);
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("given several bad fields, when submitted, then all of them are reported at once")
        void reportsEveryBadFieldTogether() {
            String token = api.tokenFor(accounts.create(Role.ORGANIZER));

            ApiClient.Response response = api.post("/api/events", token, Map.of(
                    "title", "x", "venue", "y",
                    "startsAt", futureStart().toString(),
                    "endsAt", futureStart().plusSeconds(3600).toString(),
                    "capacity", 0));

            assertThat(response.status()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.body().get("errorCode")).isEqualTo("VALIDATION_FAILED");
            @SuppressWarnings("unchecked")
            Map<String, Object> fieldErrors = (Map<String, Object>) response.body().get("fieldErrors");
            assertThat(fieldErrors).containsKeys("title", "venue", "capacity");
        }

        @Test
        @DisplayName("given malformed JSON, when submitted, then a problem response is returned")
        void rejectsMalformedJson() {
            String token = api.tokenFor(accounts.create(Role.ORGANIZER));

            ApiClient.Response response = api.postRaw("/api/events", token, "{ not json");

            assertThat(response.status()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.body().get("errorCode")).isEqualTo("MALFORMED_REQUEST");
        }

        @Test
        @DisplayName("given an internal failure would leak details, when errors are returned, "
                + "then no stack trace appears")
        void neverLeaksInternals() {
            ApiClient.Response response = api.get("/api/events/not-a-uuid", null);

            assertThat(response.raw()).doesNotContain("java.lang", "Exception", "at com.neo");
        }
    }

    @Nested
    @DisplayName("reservations over HTTP")
    class Reservations {

        @Test
        @DisplayName("given a published event, when a customer reserves, confirms and cancels, "
                + "then the seat count follows")
        void theFullReservationLifecycle() {
            String organizerToken = api.tokenFor(accounts.create(Role.ORGANIZER));
            String customerToken = api.tokenFor(accounts.create(Role.CUSTOMER));
            String eventId = publishedEvent(organizerToken, 10);

            ApiClient.Response reserved = api.post("/api/events/%s/reservations".formatted(eventId),
                    customerToken, Map.of("seats", 3), Map.of(IdempotencyHeaders.HEADER, newKey()));
            assertThat(reserved.status()).isEqualTo(HttpStatus.CREATED);
            assertThat(reserved.body().get("status")).isEqualTo("PENDING");
            String reservationId = (String) reserved.body().get("id");

            assertThat(api.get("/api/events/" + eventId, organizerToken).body())
                    .containsEntry("reservedSeats", 3)
                    .containsEntry("remainingSeats", 7);

            assertThat(api.post("/api/reservations/%s/confirm".formatted(reservationId),
                    customerToken, null).body().get("status")).isEqualTo("CONFIRMED");

            assertThat(api.post("/api/reservations/%s/cancel".formatted(reservationId),
                    customerToken, null).body().get("status")).isEqualTo("CANCELLED");

            assertThat(api.get("/api/events/" + eventId, organizerToken).body())
                    .containsEntry("reservedSeats", 0)
                    .containsEntry("remainingSeats", 10);
        }

        @Test
        @DisplayName("given no Idempotency-Key, when reserving, then the request is refused")
        void theIdempotencyKeyIsMandatory() {
            String organizerToken = api.tokenFor(accounts.create(Role.ORGANIZER));
            String customerToken = api.tokenFor(accounts.create(Role.CUSTOMER));
            String eventId = publishedEvent(organizerToken, 10);

            ApiClient.Response response = api.post("/api/events/%s/reservations".formatted(eventId),
                    customerToken, Map.of("seats", 1));

            assertThat(response.status()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.body().get("errorCode")).isEqualTo("MISSING_IDEMPOTENCY_KEY");
        }

        @Test
        @DisplayName("given the same key and body, when the request is retried, "
                + "then the original reservation is replayed")
        void retryingWithTheSameKeyReplaysTheResult() {
            String organizerToken = api.tokenFor(accounts.create(Role.ORGANIZER));
            String customerToken = api.tokenFor(accounts.create(Role.CUSTOMER));
            String eventId = publishedEvent(organizerToken, 10);
            String key = newKey();

            ApiClient.Response first = api.post("/api/events/%s/reservations".formatted(eventId),
                    customerToken, Map.of("seats", 2), Map.of(IdempotencyHeaders.HEADER, key));
            ApiClient.Response retry = api.post("/api/events/%s/reservations".formatted(eventId),
                    customerToken, Map.of("seats", 2), Map.of(IdempotencyHeaders.HEADER, key));

            assertThat(retry.status()).isEqualTo(HttpStatus.CREATED);
            assertThat(retry.body().get("id")).isEqualTo(first.body().get("id"));
            assertThat(retry.header(IdempotencyHeaders.REPLAYED_HEADER)).isEqualTo("true");
            assertThat(first.header(IdempotencyHeaders.REPLAYED_HEADER)).isEqualTo("false");

            assertThat(api.get("/api/events/" + eventId, organizerToken).body())
                    .as("the retry booked nothing extra")
                    .containsEntry("reservedSeats", 2);
        }

        @Test
        @DisplayName("given the same key with a different body, when retried, then it is refused")
        void refusesAKeyReusedForDifferentContent() {
            String organizerToken = api.tokenFor(accounts.create(Role.ORGANIZER));
            String customerToken = api.tokenFor(accounts.create(Role.CUSTOMER));
            String eventId = publishedEvent(organizerToken, 10);
            String key = newKey();

            api.post("/api/events/%s/reservations".formatted(eventId), customerToken,
                    Map.of("seats", 2), Map.of(IdempotencyHeaders.HEADER, key));
            ApiClient.Response conflicting = api.post("/api/events/%s/reservations".formatted(eventId),
                    customerToken, Map.of("seats", 5), Map.of(IdempotencyHeaders.HEADER, key));

            assertThat(conflicting.status()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(conflicting.body().get("errorCode")).isEqualTo("IDEMPOTENCY_KEY_REUSED");
        }

        @Test
        @DisplayName("given two customers using the same key, when both reserve, then neither blocks the other")
        void keysAreScopedPerUser() {
            String organizerToken = api.tokenFor(accounts.create(Role.ORGANIZER));
            String firstCustomer = api.tokenFor(accounts.create(Role.CUSTOMER));
            String secondCustomer = api.tokenFor(accounts.create(Role.CUSTOMER));
            String eventId = publishedEvent(organizerToken, 10);
            String sharedKey = newKey();

            ApiClient.Response first = api.post("/api/events/%s/reservations".formatted(eventId),
                    firstCustomer, Map.of("seats", 1), Map.of(IdempotencyHeaders.HEADER, sharedKey));
            ApiClient.Response second = api.post("/api/events/%s/reservations".formatted(eventId),
                    secondCustomer, Map.of("seats", 1), Map.of(IdempotencyHeaders.HEADER, sharedKey));

            assertThat(first.status()).isEqualTo(HttpStatus.CREATED);
            assertThat(second.status()).isEqualTo(HttpStatus.CREATED);
            assertThat(second.body().get("id")).isNotEqualTo(first.body().get("id"));
        }

        @Test
        @DisplayName("given more seats than remain, when reserving, then the shortfall is reported")
        void refusesToOversellOverHttp() {
            String organizerToken = api.tokenFor(accounts.create(Role.ORGANIZER));
            String customerToken = api.tokenFor(accounts.create(Role.CUSTOMER));
            String eventId = publishedEvent(organizerToken, 2);

            ApiClient.Response response = api.post("/api/events/%s/reservations".formatted(eventId),
                    customerToken, Map.of("seats", 3), Map.of(IdempotencyHeaders.HEADER, newKey()));

            assertThat(response.status()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.body())
                    .containsEntry("errorCode", "INSUFFICIENT_CAPACITY")
                    .containsEntry("remainingSeats", 2);
        }

        @Test
        @DisplayName("given an unpublished event, when reserving, then it is refused")
        void refusesReservationsOnDrafts() {
            String organizerToken = api.tokenFor(accounts.create(Role.ORGANIZER));
            String customerToken = api.tokenFor(accounts.create(Role.CUSTOMER));
            String draftId = (String) api.post("/api/events", organizerToken, eventBody(10))
                    .body().get("id");

            ApiClient.Response response = api.post("/api/events/%s/reservations".formatted(draftId),
                    customerToken, Map.of("seats", 1), Map.of(IdempotencyHeaders.HEADER, newKey()));

            assertThat(response.status()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.body().get("errorCode")).isEqualTo("EVENT_NOT_PUBLISHED");
        }

        @Test
        @DisplayName("given another customer's reservation, when acting on it, then it is forbidden")
        void reservationsArePrivateToTheirHolder() {
            String organizerToken = api.tokenFor(accounts.create(Role.ORGANIZER));
            String holderToken = api.tokenFor(accounts.create(Role.CUSTOMER));
            String intruderToken = api.tokenFor(accounts.create(Role.CUSTOMER));
            String eventId = publishedEvent(organizerToken, 10);

            String reservationId = (String) api.post("/api/events/%s/reservations".formatted(eventId),
                    holderToken, Map.of("seats", 1), Map.of(IdempotencyHeaders.HEADER, newKey()))
                    .body().get("id");

            assertThat(api.get("/api/reservations/" + reservationId, intruderToken).status())
                    .isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(api.post("/api/reservations/%s/cancel".formatted(reservationId),
                    intruderToken, null).status()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("given a seat count above the per-reservation cap, when reserving, then it is rejected")
        void enforcesThePerReservationCap() {
            String organizerToken = api.tokenFor(accounts.create(Role.ORGANIZER));
            String customerToken = api.tokenFor(accounts.create(Role.CUSTOMER));
            String eventId = publishedEvent(organizerToken, 100);

            ApiClient.Response response = api.post("/api/events/%s/reservations".formatted(eventId),
                    customerToken, Map.of("seats", 50), Map.of(IdempotencyHeaders.HEADER, newKey()));

            assertThat(response.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("documentation")
    class Documentation {

        @Test
        @DisplayName("given the service is running, when the OpenAPI document is fetched, "
                + "then it describes the API")
        void servesAnOpenApiDocument() {
            ApiClient.Response response = api.get("/v3/api-docs", null);

            assertThat(response.status()).isEqualTo(HttpStatus.OK);
            @SuppressWarnings("unchecked")
            Map<String, Object> paths = (Map<String, Object>) response.body().get("paths");
            assertThat(paths).containsKeys(
                    "/api/auth/login", "/api/auth/refresh", "/api/events",
                    "/api/events/{id}/publish", "/api/events/public",
                    "/api/events/{eventId}/reservations",
                    "/api/reservations/{id}/confirm", "/api/reservations/{id}/cancel");
        }
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }
}
