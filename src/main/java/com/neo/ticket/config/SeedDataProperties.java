package com.neo.ticket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "neo.seed")
public record SeedDataProperties(boolean enabled, String password) {
}
