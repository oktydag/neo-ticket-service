package com.neo.ticket.shared.web;

public record RequestMetadata(String clientIp, String userAgent, String requestId) {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    public static final int MAX_USER_AGENT_LENGTH = 256;

    public static final String UNKNOWN = "unknown";

    public static RequestMetadata unknown() {
        return new RequestMetadata(UNKNOWN, UNKNOWN, UNKNOWN);
    }
}
