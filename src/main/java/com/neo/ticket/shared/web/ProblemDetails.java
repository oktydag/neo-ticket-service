package com.neo.ticket.shared.web;

import com.neo.ticket.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

public final class ProblemDetails {

    private static final String TYPE_PREFIX = "https://neo.example/problems/";

    private ProblemDetails() {
    }

    public static HttpStatus statusFor(ErrorCode errorCode) {
        return switch (errorCode.category()) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case AUTHENTICATION -> HttpStatus.UNAUTHORIZED;
            case AUTHORIZATION -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case RATE_LIMIT -> HttpStatus.TOO_MANY_REQUESTS;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    public static ProblemDetail of(ErrorCode errorCode, String detail) {
        return of(errorCode, detail, Map.of());
    }

    public static ProblemDetail of(ErrorCode errorCode, String detail, Map<String, Object> properties) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(statusFor(errorCode), detail);
        problem.setType(URI.create(TYPE_PREFIX + slug(errorCode)));
        problem.setTitle(errorCode.title());
        problem.setProperty("errorCode", errorCode.name());
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("requestId", RequestMetadataHolder.current().requestId());
        properties.forEach(problem::setProperty);
        return problem;
    }

    private static String slug(ErrorCode errorCode) {
        return errorCode.name().toLowerCase().replace('_', '-');
    }
}
