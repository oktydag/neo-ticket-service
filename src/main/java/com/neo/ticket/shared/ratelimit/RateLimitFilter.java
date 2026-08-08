package com.neo.ticket.shared.ratelimit;

import com.neo.ticket.shared.error.ErrorCode;
import com.neo.ticket.shared.web.ProblemDetailWriter;
import com.neo.ticket.shared.web.RequestMetadataHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final String AUTH_PATH_PREFIX = "/api/auth/";
    private static final String AUTHENTICATION_POLICY = "authentication";
    private static final String GENERAL_POLICY = "general";

    private static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String LIMIT_HEADER = "X-RateLimit-Limit";

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final ProblemDetailWriter problemDetailWriter;

    public RateLimitFilter(RateLimiter rateLimiter, RateLimitProperties properties,
                           ProblemDetailWriter problemDetailWriter) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.problemDetailWriter = problemDetailWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        boolean authenticationEndpoint = request.getRequestURI().startsWith(AUTH_PATH_PREFIX);
        String policyName = authenticationEndpoint ? AUTHENTICATION_POLICY : GENERAL_POLICY;
        RateLimitProperties.Policy policy =
                authenticationEndpoint ? properties.authentication() : properties.general();

        String clientKey = RequestMetadataHolder.current().clientIp();
        RateLimiter.Decision decision = rateLimiter.check(policyName, clientKey, policy);

        response.setHeader(LIMIT_HEADER, String.valueOf(policy.capacity()));
        response.setHeader(REMAINING_HEADER, String.valueOf(decision.remainingQuota()));

        if (decision.allowed()) {
            chain.doFilter(request, response);
            return;
        }
        log.info("Rate limit hit on {} policy for {} {}", policyName,
                request.getMethod(), request.getRequestURI());
        writeTooManyRequests(response, decision);
    }

    private void writeTooManyRequests(HttpServletResponse response, RateLimiter.Decision decision)
            throws IOException {
        long retryAfterSeconds = Math.max(1, decision.retryAfter().toSeconds());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        problemDetailWriter.write(response, ErrorCode.RATE_LIMIT_EXCEEDED,
                "Too many requests. Retry in %d second(s).".formatted(retryAfterSeconds),
                Map.of("retryAfterSeconds", retryAfterSeconds));
    }
}
