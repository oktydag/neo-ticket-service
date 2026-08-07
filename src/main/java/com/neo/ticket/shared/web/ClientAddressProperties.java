package com.neo.ticket.shared.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "neo.client-address")
public record ClientAddressProperties(boolean trustForwardedHeaders) {
}
