package com.neo.ticket.shared.error;

import java.util.Map;

public class ForbiddenOperationException extends DomainException {

    public ForbiddenOperationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ForbiddenOperationException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }
}
