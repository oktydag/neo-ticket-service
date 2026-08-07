package com.neo.ticket.shared.error;

public enum ErrorCode {

    VALIDATION_FAILED(ErrorCategory.VALIDATION, "Request validation failed"),
    MALFORMED_REQUEST(ErrorCategory.VALIDATION, "Malformed request"),
    INVARIANT_VIOLATION(ErrorCategory.VALIDATION, "Invalid value"),
    MISSING_IDEMPOTENCY_KEY(ErrorCategory.VALIDATION, "Idempotency-Key header is required"),

    AUTHENTICATION_REQUIRED(ErrorCategory.AUTHENTICATION, "Authentication required"),
    INVALID_CREDENTIALS(ErrorCategory.AUTHENTICATION, "Invalid e-mail or password"),
    INVALID_TOKEN(ErrorCategory.AUTHENTICATION, "Token is invalid or expired"),
    REFRESH_TOKEN_REUSED(ErrorCategory.AUTHENTICATION, "Refresh token has already been used"),

    ACCESS_DENIED(ErrorCategory.AUTHORIZATION, "Access denied"),
    NOT_RESOURCE_OWNER(ErrorCategory.AUTHORIZATION, "Only the owner or an administrator may do this"),

    USER_NOT_FOUND(ErrorCategory.NOT_FOUND, "User not found"),
    EVENT_NOT_FOUND(ErrorCategory.NOT_FOUND, "Event not found"),
    RESERVATION_NOT_FOUND(ErrorCategory.NOT_FOUND, "Reservation not found"),

    EMAIL_ALREADY_REGISTERED(ErrorCategory.CONFLICT, "E-mail is already registered"),
    EVENT_ALREADY_PUBLISHED(ErrorCategory.CONFLICT, "Event is already published"),
    EVENT_NOT_PUBLISHED(ErrorCategory.CONFLICT, "Event is not published yet"),
    EVENT_NOT_RESERVABLE(ErrorCategory.CONFLICT, "Event is not open for reservations"),
    EVENT_IMMUTABLE_AFTER_PUBLISH(ErrorCategory.CONFLICT, "Field cannot be changed after publication"),
    INSUFFICIENT_CAPACITY(ErrorCategory.CONFLICT, "Not enough seats left"),
    CAPACITY_BELOW_RESERVED(ErrorCategory.CONFLICT, "Capacity cannot drop below reserved seats"),
    ILLEGAL_STATUS_TRANSITION(ErrorCategory.CONFLICT, "Illegal status transition"),
    CONCURRENT_MODIFICATION(ErrorCategory.CONFLICT, "Resource was modified concurrently"),
    IDEMPOTENCY_KEY_REUSED(ErrorCategory.CONFLICT, "Idempotency-Key was reused with a different payload"),
    IDEMPOTENT_REQUEST_IN_PROGRESS(ErrorCategory.CONFLICT, "An identical request is still in progress"),
    IDEMPOTENT_RESULT_UNAVAILABLE(ErrorCategory.CONFLICT, "The original response can no longer be replayed"),

    RATE_LIMIT_EXCEEDED(ErrorCategory.RATE_LIMIT, "Too many requests"),

    INTERNAL_ERROR(ErrorCategory.INTERNAL, "Unexpected internal error");

    private final ErrorCategory category;
    private final String title;

    ErrorCode(ErrorCategory category, String title) {
        this.category = category;
        this.title = title;
    }

    public ErrorCategory category() {
        return category;
    }

    public String title() {
        return title;
    }
}
