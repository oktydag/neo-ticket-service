package com.neo.ticket.idempotency.application;

import com.neo.ticket.shared.domain.Hashing;
import com.neo.ticket.shared.error.BusinessRuleViolationException;
import com.neo.ticket.shared.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;

@Service
public class IdempotencyGuard {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyGuard.class);

    private final IdempotencyRecordStore store;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final IdempotencyProperties properties;

    public IdempotencyGuard(IdempotencyRecordStore store, ObjectMapper objectMapper,
                            Clock clock, IdempotencyProperties properties) {
        this.store = store;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.properties = properties;
    }

    public <T> IdempotentOutcome<T> execute(IdempotencyContext context, Object requestPayload,
                                            int successStatus, Class<T> responseType,
                                            Supplier<T> operation) {
        Instant now = clock.instant();
        String requestHash = fingerprint(requestPayload);
        IdempotencyDecision decision = claim(context, requestHash, now);

        return switch (decision) {
            case IdempotencyDecision.Proceed ignored ->
                    new IdempotentOutcome<>(run(context, operation, successStatus), successStatus, false);

            case IdempotencyDecision.Replay(int status, String body) -> {
                log.debug("Replaying stored response for idempotency key on {}", context.endpoint());
                yield new IdempotentOutcome<>(deserialise(body, responseType), status, true);
            }

            case IdempotencyDecision.InProgress ignored -> throw new BusinessRuleViolationException(
                    ErrorCode.IDEMPOTENT_REQUEST_IN_PROGRESS,
                    "A request with this Idempotency-Key is still being processed; retry shortly");

            case IdempotencyDecision.PayloadMismatch ignored -> throw new BusinessRuleViolationException(
                    ErrorCode.IDEMPOTENCY_KEY_REUSED,
                    "This Idempotency-Key was already used for a request with a different body");

            case IdempotencyDecision.ResultUnavailable ignored -> throw new BusinessRuleViolationException(
                    ErrorCode.IDEMPOTENT_RESULT_UNAVAILABLE,
                    "The original request succeeded but its response was not retained; "
                            + "query the resource directly");
        };
    }

    private IdempotencyDecision claim(IdempotencyContext context, String requestHash, Instant now) {
        try {
            store.insertClaim(context, requestHash, now, properties.ttl());
            return new IdempotencyDecision.Proceed();
        } catch (DataIntegrityViolationException alreadyClaimed) {
            return store.inspect(context, requestHash, now, properties.ttl());
        }
    }

    private <T> T run(IdempotencyContext context, Supplier<T> operation, int successStatus) {
        T result;
        try {
            result = operation.get();
        } catch (RuntimeException failure) {
            store.markFailed(context);
            throw failure;
        }
        store.complete(context, serialise(result), successStatus);
        return result;
    }

    private String fingerprint(Object requestPayload) {
        return Hashing.sha256Hex(serialise(requestPayload));
    }

    private String serialise(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException cause) {
            throw new IllegalStateException("Payload could not be serialised for idempotency", cause);
        }
    }

    private <T> T deserialise(String body, Class<T> responseType) {
        try {
            return objectMapper.readValue(body, responseType);
        } catch (JacksonException cause) {
            throw new IllegalStateException("Stored idempotent response could not be read back", cause);
        }
    }
}
