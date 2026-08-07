package com.neo.ticket.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestMetadataFilter extends OncePerRequestFilter {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String USER_AGENT_HEADER = "User-Agent";

    private final boolean trustForwardedHeaders;

    public RequestMetadataFilter(ClientAddressProperties properties) {
        this.trustForwardedHeaders = properties.trustForwardedHeaders();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        RequestMetadataHolder.set(new RequestMetadata(
                resolveClientIp(request), resolveUserAgent(request), requestId));
        response.setHeader(RequestMetadata.REQUEST_ID_HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            RequestMetadataHolder.clear();
        }
    }

    private static String resolveRequestId(HttpServletRequest request) {
        String supplied = request.getHeader(RequestMetadata.REQUEST_ID_HEADER);
        return supplied != null && !supplied.isBlank()
                ? supplied.strip()
                : UUID.randomUUID().toString();
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String forwarded = request.getHeader(FORWARDED_FOR_HEADER);
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].strip();
            }
        }
        String remoteAddress = request.getRemoteAddr();
        return remoteAddress != null ? remoteAddress : RequestMetadata.UNKNOWN;
    }

    private static String resolveUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader(USER_AGENT_HEADER);
        if (userAgent == null || userAgent.isBlank()) {
            return RequestMetadata.UNKNOWN;
        }
        return userAgent.length() <= RequestMetadata.MAX_USER_AGENT_LENGTH
                ? userAgent
                : userAgent.substring(0, RequestMetadata.MAX_USER_AGENT_LENGTH);
    }
}
