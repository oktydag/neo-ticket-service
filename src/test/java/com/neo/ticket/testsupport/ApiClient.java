package com.neo.ticket.testsupport;

import com.neo.ticket.iam.domain.User;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class ApiClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient restClient;

    public ApiClient(int port) {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (request, response) -> { })
                .build();
    }

    public String tokenFor(User user) {
        Response response = post("/api/auth/login", null,
                Map.of("email", user.email().value(), "password", TestAccounts.PASSWORD));
        if (response.status() != HttpStatus.OK) {
            throw new IllegalStateException("Could not sign in test user: " + response.raw());
        }
        return (String) response.body().get("accessToken");
    }

    public Response get(String path, String token) {
        return get(path, token, Map.of());
    }

    public Response get(String path, String token, Map<String, String> extraHeaders) {
        return exchange(HttpMethod.GET, path, token, null, extraHeaders);
    }

    public Response post(String path, String token, Object body) {
        return post(path, token, body, Map.of());
    }

    public Response post(String path, String token, Object body, Map<String, String> extraHeaders) {
        return exchange(HttpMethod.POST, path, token, writeJson(body), extraHeaders);
    }

    public Response postRaw(String path, String token, String rawBody) {
        return exchange(HttpMethod.POST, path, token, rawBody, Map.of());
    }

    public Response put(String path, String token, Object body) {
        return exchange(HttpMethod.PUT, path, token, writeJson(body), Map.of());
    }

    private Response exchange(HttpMethod method, String path, String token,
                              String body, Map<String, String> extraHeaders) {
        RestClient.RequestBodySpec request = restClient.method(method)
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PROBLEM_JSON);
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        extraHeaders.forEach(request::header);
        if (body != null) {
            request.body(body);
        }

        ResponseEntity<String> response = request.retrieve().toEntity(String.class);
        return new Response(HttpStatus.valueOf(response.getStatusCode().value()),
                response.getBody(), response.getHeaders());
    }

    private static String writeJson(Object body) {
        return body == null ? null : JSON.writeValueAsString(body);
    }

    public record Response(HttpStatus status, String raw, HttpHeaders headers) {

        @SuppressWarnings("unchecked")
        public Map<String, Object> body() {
            if (raw == null || raw.isBlank()) {
                return Map.of();
            }
            return JSON.readValue(raw, Map.class);
        }

        @SuppressWarnings("unchecked")
        public List<Map<String, Object>> list(String field) {
            return (List<Map<String, Object>>) body().get(field);
        }

        public String header(String name) {
            return headers.getFirst(name);
        }

        public String contentType() {
            return String.valueOf(headers.getContentType());
        }
    }
}
