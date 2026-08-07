package com.neo.ticket.idempotency.application;

import com.neo.ticket.shared.domain.Invariants;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.web.IdempotencyHeaders;

public record IdempotencyContext(String key, String endpoint, UserId userId) {

    public IdempotencyContext {
        key = Invariants.requireText(key, IdempotencyHeaders.HEADER,
                IdempotencyHeaders.MIN_KEY_LENGTH, IdempotencyHeaders.MAX_KEY_LENGTH);
        Invariants.requireText(endpoint, "endpoint", 1, 200);
        Invariants.requirePresent(userId, "userId");
    }
}
