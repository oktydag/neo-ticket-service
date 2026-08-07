package com.neo.ticket.shared.error;

import java.util.Map;

public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessRuleViolationException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }
}
