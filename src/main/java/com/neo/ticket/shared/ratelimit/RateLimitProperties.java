package com.neo.ticket.shared.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "neo.rate-limit")
public record RateLimitProperties(boolean enabled, Policy authentication, Policy general) {

    public record Policy(int capacity, Duration refillPeriod) {
    }
}
