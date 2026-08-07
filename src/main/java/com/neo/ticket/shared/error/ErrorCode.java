package com.neo.ticket.shared.error;

public enum ErrorCode {

    INVARIANT_VIOLATION(ErrorCategory.VALIDATION, "Invalid value"),

    ACCESS_DENIED(ErrorCategory.AUTHORIZATION, "Access denied"),
    NOT_RESOURCE_OWNER(ErrorCategory.AUTHORIZATION, "Only the owner or an administrator may do this"),

    EVENT_ALREADY_PUBLISHED(ErrorCategory.CONFLICT, "Event is already published"),
    EVENT_NOT_PUBLISHED(ErrorCategory.CONFLICT, "Event is not published yet"),
    EVENT_NOT_RESERVABLE(ErrorCategory.CONFLICT, "Event is not open for reservations"),

    INSUFFICIENT_CAPACITY(ErrorCategory.CONFLICT, "Not enough seats left"),
    CAPACITY_BELOW_RESERVED(ErrorCategory.CONFLICT, "Capacity cannot drop below reserved seats"),

    EVENT_IMMUTABLE_AFTER_PUBLISH(ErrorCategory.CONFLICT, "Field cannot be changed after publication");

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
