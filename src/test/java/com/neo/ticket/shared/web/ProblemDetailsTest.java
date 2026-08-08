package com.neo.ticket.shared.web;

import com.neo.ticket.shared.error.ErrorCategory;
import com.neo.ticket.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProblemDetails")
class ProblemDetailsTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(ErrorCode.class)
    @DisplayName("given any error code, when mapped, then it yields a sensible HTTP status")
    void mapsEveryCodeToAStatus(ErrorCode errorCode) {
        HttpStatus status = ProblemDetails.statusFor(errorCode);

        assertThat(status).isNotNull();
        assertThat(status.isError()).isTrue();
    }

    @Test
    @DisplayName("given each category, when mapped, then the expected status is produced")
    void mapsCategoriesAsDocumented() {
        assertThat(ProblemDetails.statusFor(ErrorCode.VALIDATION_FAILED)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ProblemDetails.statusFor(ErrorCode.INVALID_CREDENTIALS)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ProblemDetails.statusFor(ErrorCode.ACCESS_DENIED)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ProblemDetails.statusFor(ErrorCode.EVENT_NOT_FOUND)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ProblemDetails.statusFor(ErrorCode.INSUFFICIENT_CAPACITY)).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ProblemDetails.statusFor(ErrorCode.RATE_LIMIT_EXCEEDED)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(ProblemDetails.statusFor(ErrorCode.INTERNAL_ERROR))
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ErrorCode.class)
    @DisplayName("given any error code, when it carries a title, then the title is non-blank")
    void everyCodeIsDescribed(ErrorCode errorCode) {
        assertThat(errorCode.title()).isNotBlank();
        assertThat(errorCode.category()).isInstanceOf(ErrorCategory.class);
    }

    @Test
    @DisplayName("given a code and detail, when built, then the body carries the machine-readable code")
    void buildsAProblemBody() {
        ProblemDetail problem = ProblemDetails.of(ErrorCode.INSUFFICIENT_CAPACITY,
                "Only 1 seat left", Map.of("remainingSeats", 1));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo(ErrorCode.INSUFFICIENT_CAPACITY.title());
        assertThat(problem.getDetail()).isEqualTo("Only 1 seat left");
        assertThat(problem.getProperties())
                .containsEntry("errorCode", "INSUFFICIENT_CAPACITY")
                .containsEntry("remainingSeats", 1)
                .containsKey("timestamp")
                .containsKey("requestId");
        assertThat(problem.getType().toString())
                .isEqualTo("https://neo.example/problems/insufficient-capacity");
    }
}
