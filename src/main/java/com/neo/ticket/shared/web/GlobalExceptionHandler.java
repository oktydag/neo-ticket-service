package com.neo.ticket.shared.web;

import com.neo.ticket.shared.error.DomainException;
import com.neo.ticket.shared.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException exception) {
        log.debug("Domain rule rejected the request: {} - {}",
                exception.errorCode(), exception.getMessage());
        return ProblemDetails.of(exception.errorCode(), exception.getMessage(), exception.details());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleBeanValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), defaultMessage(error.getDefaultMessage())));
        exception.getBindingResult().getGlobalErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getObjectName(), defaultMessage(error.getDefaultMessage())));
        return ProblemDetails.of(ErrorCode.VALIDATION_FAILED,
                "One or more fields are invalid", Map.of("fieldErrors", fieldErrors));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleParameterValidation(HandlerMethodValidationException exception) {
        List<String> messages = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(error -> defaultMessage(error.getDefaultMessage()))
                .toList();
        return ProblemDetails.of(ErrorCode.VALIDATION_FAILED,
                "One or more parameters are invalid", Map.of("violations", messages));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
        log.debug("Rejected unreadable request body", exception);
        return ProblemDetails.of(ErrorCode.MALFORMED_REQUEST,
                "Request body is missing or is not valid JSON");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ProblemDetails.of(ErrorCode.MALFORMED_REQUEST,
                "Parameter '%s' has an unexpected format".formatted(exception.getName()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingHeader(MissingRequestHeaderException exception) {
        ErrorCode errorCode = IdempotencyHeaders.HEADER.equalsIgnoreCase(exception.getHeaderName())
                ? ErrorCode.MISSING_IDEMPOTENCY_KEY
                : ErrorCode.MALFORMED_REQUEST;
        return ProblemDetails.of(errorCode,
                "Required header '%s' is missing".formatted(exception.getHeaderName()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException exception) {
        return ProblemDetails.of(ErrorCode.EVENT_NOT_FOUND,
                "No endpoint matches %s".formatted(exception.getResourcePath()));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException exception) {
        log.debug("Optimistic lock conflict", exception);
        return ProblemDetails.of(ErrorCode.CONCURRENT_MODIFICATION,
                "The resource was modified by another request; reload it and try again");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Database rejected the write as inconsistent", exception);
        return ProblemDetails.of(ErrorCode.CONCURRENT_MODIFICATION,
                "The request conflicts with existing data");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException exception) {
        return ProblemDetails.of(ErrorCode.ACCESS_DENIED,
                "You are not allowed to perform this operation");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException exception) {
        return ProblemDetails.of(ErrorCode.AUTHENTICATION_REQUIRED,
                "Valid authentication credentials are required");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unhandled exception escaped a controller", exception);
        return ProblemDetails.of(ErrorCode.INTERNAL_ERROR,
                "The request could not be processed. Quote the requestId when reporting this.");
    }

    private static String defaultMessage(String message) {
        return message != null ? message : "is invalid";
    }
}
