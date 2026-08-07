package com.neo.ticket.shared.error;

public class InvariantViolationException extends DomainException {

    public InvariantViolationException(String message) {
        super(ErrorCode.INVARIANT_VIOLATION, message);
    }

    public InvariantViolationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
