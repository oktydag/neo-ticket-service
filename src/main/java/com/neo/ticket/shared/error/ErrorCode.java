package com.neo.ticket.shared.error;

public enum ErrorCode {

    VALIDATION_FAILED(ErrorCategory.VALIDATION, "Request validation failed"),
    MALFORMED_REQUEST(ErrorCategory.VALIDATION, "Malformed request"),
    INVARIANT_VIOLATION(ErrorCategory.VALIDATION, "Invalid value"),
    MISSING_IDEMPOTENCY_KEY(ErrorCategory.VALIDATION, "Idempotency-Key header is required"),

    AUTHENTICATION_REQUIRED(ErrorCategory.AUTHENTICATION, "Authentication required"),

    ACCESS_DENIED(ErrorCategory.AUTHORIZATION, "Access denied"),
    NOT_RESOURCE_OWNER(ErrorCategory.AUTHORIZATION, "Only the owner or an administrator may do this"),

    EVENT_NOT_FOUND(ErrorCategory.NOT_FOUND, "Event not found"),

    EVENT_ALREADY_PUBLISHED(ErrorCategory.CONFLICT, "Event is already published"),
    EVENT_NOT_PUBLISHED(ErrorCategory.CONFLICT, "Event is not published yet"),
    EVENT_NOT_RESERVABLE(ErrorCategory.CONFLICT, "Event is not open for reservations"),
    EVENT_IMMUTABLE_AFTER_PUBLISH(ErrorCategory.CONFLICT, "Field cannot be changed after publication"),
    INSUFFICIENT_CAPACITY(ErrorCategory.CONFLICT, "Not enough seats left"),
    CAPACITY_BELOW_RESERVED(ErrorCategory.CONFLICT, "Capacity cannot drop below reserved seats"),
    CONCURRENT_MODIFICATION(ErrorCategory.CONFLICT, "Resource was modified concurrently"),


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
