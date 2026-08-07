package com.neo.ticket.iam.application.command.handlers;

import com.neo.ticket.iam.application.TokenIssuer;
import com.neo.ticket.iam.application.TokenPair;
import com.neo.ticket.iam.domain.*;
import com.neo.ticket.shared.application.DomainEventPublisher;
import com.neo.ticket.shared.error.AuthenticationFailedException;
import com.neo.ticket.shared.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenHandler {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenHandler.class);

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final TokenIssuer tokenIssuer;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    public RefreshTokenHandler(UserRepository users,
                               RefreshTokenRepository refreshTokens,
                               TokenIssuer tokenIssuer,
                               DomainEventPublisher domainEventPublisher,
                               Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.tokenIssuer = tokenIssuer;
        this.domainEventPublisher = domainEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public TokenPair handle(String presentedToken) {
        Instant now = clock.instant();
        TokenIssuer.RefreshTokenClaims claims = tokenIssuer.decodeRefreshToken(presentedToken);

        RefreshToken stored = refreshTokens.findByTokenId(claims.tokenId())
                .orElseThrow(() -> {
                    log.info("Refresh token {} is not on record", claims.tokenId());
                    return new AuthenticationFailedException(ErrorCode.INVALID_TOKEN,
                            "Refresh token is no longer valid; sign in again");
                });

        if (stored.isRevoked()) {
            throw handleReplay(stored, now);
        }
        if (stored.isExpiredAt(now)) {
            throw new AuthenticationFailedException(ErrorCode.INVALID_TOKEN,
                    "Refresh token has expired; sign in again");
        }

        User user = users.findById(stored.userId())
                .orElseThrow(() -> new AuthenticationFailedException(ErrorCode.INVALID_TOKEN,
                        "The account behind this token no longer exists"));

        UUID successorId = UUID.randomUUID();
        TokenIssuer.IssuedToken refreshToken = tokenIssuer.issueRefreshToken(user.id(), successorId, now);
        RefreshToken successor = stored.rotate(successorId, now, refreshToken.expiresAt());
        refreshTokens.save(stored);
        refreshTokens.save(successor);

        TokenIssuer.IssuedToken accessToken = tokenIssuer.issueAccessToken(user.id(), user.roles(), now);
        return TokenPair.of(accessToken, refreshToken, now);
    }

    private AuthenticationFailedException handleReplay(RefreshToken spent, Instant now) {
        int revoked = refreshTokens.revokeFamily(spent.familyId(), now);
        domainEventPublisher.publish(new AuthenticationEvents.RefreshTokenReuseDetected(
                spent.userId(), spent.familyId(), revoked, now));
        log.warn("Refresh token reuse detected for family {}; revoked {} token(s)",
                spent.familyId(), revoked);
        return new AuthenticationFailedException(ErrorCode.REFRESH_TOKEN_REUSED,
                "This refresh token was already used. All sessions have been ended; sign in again.");
    }
}
