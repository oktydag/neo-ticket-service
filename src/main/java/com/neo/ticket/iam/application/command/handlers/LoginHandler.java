package com.neo.ticket.iam.application.command.handlers;

import com.neo.ticket.iam.application.TokenIssuer;
import com.neo.ticket.iam.application.TokenPair;
import com.neo.ticket.iam.application.command.LoginCommand;
import com.neo.ticket.iam.domain.*;
import com.neo.ticket.iam.domain.valueobject.Email;
import com.neo.ticket.iam.domain.valueobject.PasswordHash;
import com.neo.ticket.iam.domain.valueobject.RawPassword;
import com.neo.ticket.shared.application.DomainEventPublisher;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.error.AuthenticationFailedException;
import com.neo.ticket.shared.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class LoginHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginHandler.class);

    private static final int DECOY_SECRET_BYTES = 32;

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    private final PasswordHash decoyHash;

    public LoginHandler(UserRepository users,
                        RefreshTokenRepository refreshTokens,
                        PasswordHasher passwordHasher,
                        TokenIssuer tokenIssuer,
                        DomainEventPublisher domainEventPublisher,
                        Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.domainEventPublisher = domainEventPublisher;
        this.clock = clock;
        this.decoyHash = passwordHasher.hash(randomSecret());
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public TokenPair handle(LoginCommand command) {
        Instant now = clock.instant();
        Email email = Email.of(command.email());
        Optional<User> found = users.findByEmail(email);

        if (!passwordMatches(command.password(), found)) {
            domainEventPublisher.publish(new AuthenticationEvents.LoginFailed(email.value(), now));
            log.info("Rejected sign-in attempt");
            throw invalidCredentials();
        }

        User user = found.orElseThrow(LoginHandler::invalidCredentials);
        user.recordSuccessfulLogin(now);
        users.save(user);

        TokenPair tokens = issueFreshFamily(user.id(), user.roles(), now);
        domainEventPublisher.publish(new AuthenticationEvents.UserLoggedIn(user.id(), now));
        return tokens;
    }

    private boolean passwordMatches(String candidate, Optional<User> user) {
        RawPassword rawPassword;
        try {
            rawPassword = RawPassword.of(candidate);
        } catch (RuntimeException tooShortOrMissing) {

            passwordHasher.matches(randomSecret(), decoyHash);
            return false;
        }
        return user.map(found -> passwordHasher.matches(rawPassword, found.passwordHash()))
                .orElseGet(() -> {
                    passwordHasher.matches(rawPassword, decoyHash);
                    return false;
                });
    }

    private TokenPair issueFreshFamily(UserId userId, Set<Role> roles, Instant now) {
        UUID tokenId = UUID.randomUUID();
        TokenIssuer.IssuedToken refreshToken = tokenIssuer.issueRefreshToken(userId, tokenId, now);
        refreshTokens.save(RefreshToken.issueNewFamily(
                tokenId, userId, now, refreshToken.expiresAt()));

        TokenIssuer.IssuedToken accessToken = tokenIssuer.issueAccessToken(userId, roles, now);
        return TokenPair.of(accessToken, refreshToken, now);
    }

    private static AuthenticationFailedException invalidCredentials() {
        return new AuthenticationFailedException(ErrorCode.INVALID_CREDENTIALS,
                "E-mail or password is incorrect");
    }

    private static RawPassword randomSecret() {
        byte[] bytes = new byte[DECOY_SECRET_BYTES];
        new SecureRandom().nextBytes(bytes);
        return RawPassword.of(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
                .substring(0, RawPassword.MIN_LENGTH + 8));
    }
}
