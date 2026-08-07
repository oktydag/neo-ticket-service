package com.neo.ticket.iam.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
class JwtSecretGuard {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretGuard.class);

    private static final Set<String> NON_PRODUCTION_PROFILES = Set.of("dev", "test", "local");

    private final JwtProperties properties;
    private final Environment environment;

    JwtSecretGuard(JwtProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    void verifySecretIsNotTheSharedDefault() {
        if (!properties.usesDevelopmentSecret()) {
            return;
        }
        List<String> activeProfiles = profilesInEffect();
        boolean development = !activeProfiles.isEmpty()
                && NON_PRODUCTION_PROFILES.containsAll(activeProfiles);

        if (development) {
            log.warn("Running with the built-in development JWT secret. "
                    + "Set NEO_JWT_SECRET before exposing this service to anyone else.");
            return;
        }
        throw new IllegalStateException("""
                Refusing to start: neo.security.jwt.secret is still the development \
                default, and the active profiles %s are not development-only. \
                Set the NEO_JWT_SECRET environment variable to a random value of at \
                least %d characters.""".formatted(activeProfiles, JwtProperties.MIN_SECRET_LENGTH));
    }

    private List<String> profilesInEffect() {
        List<String> active = List.of(environment.getActiveProfiles());
        return active.isEmpty() ? List.of(environment.getDefaultProfiles()) : active;
    }
}
