package com.neo.ticket.shared.error;

public class AuthenticationFailedException extends DomainException {

    public AuthenticationFailedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
