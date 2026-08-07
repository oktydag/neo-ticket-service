package com.neo.ticket.idempotency.application;

import com.neo.ticket.idempotency.domain.IdempotencyRecord;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "neo.idempotency")
public record IdempotencyProperties(Duration ttl) {

    public IdempotencyProperties {
        if (ttl == null) {
            ttl = IdempotencyRecord.DEFAULT_TTL;
        }
    }
}
