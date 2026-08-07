package com.neo.ticket.shared.error;

import java.util.Map;

public abstract class DomainException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Map<String, Object> details;

    protected DomainException(ErrorCode errorCode, String message) {
        this(errorCode, message, Map.of());
    }

    protected DomainException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = Map.copyOf(details);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Map<String, Object> details() {
        return details;
    }
}
