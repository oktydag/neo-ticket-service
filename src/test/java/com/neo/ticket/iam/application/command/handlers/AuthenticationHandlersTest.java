package com.neo.ticket.iam.application.command.handlers;

import com.neo.ticket.iam.application.TokenPair;
import com.neo.ticket.iam.application.UserView;
import com.neo.ticket.iam.application.command.LoginCommand;
import com.neo.ticket.iam.application.command.RegisterUserCommand;
import com.neo.ticket.iam.domain.AuthenticationEvents;
import com.neo.ticket.iam.domain.RefreshToken;
import com.neo.ticket.iam.domain.User;
import com.neo.ticket.iam.domain.UserRegistered;
import com.neo.ticket.iam.domain.valueobject.Email;
import com.neo.ticket.iam.domain.valueobject.RawPassword;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.error.AuthenticationFailedException;
import com.neo.ticket.shared.error.BusinessRuleViolationException;
import com.neo.ticket.shared.error.DomainException;
import com.neo.ticket.shared.error.ErrorCode;
import com.neo.ticket.testsupport.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

import static com.neo.ticket.testsupport.TestFixtures.FIXED_CLOCK;
import static com.neo.ticket.testsupport.TestFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Authentication handlers")
class AuthenticationHandlersTest {

    private static final String PASSWORD = "a-long-enough-password";
    private static final String EMAIL = "ada@neo.io";

    private InMemoryUserRepository users;
    private InMemoryRefreshTokenRepository refreshTokens;
    private FakePasswordHasher passwordHasher;
    private FakeTokenIssuer tokenIssuer;
    private RecordingEventPublisher events;

    private RegisterUserHandler registerHandler;
    private LoginHandler loginHandler;
    private RefreshTokenHandler refreshTokenHandler;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        refreshTokens = new InMemoryRefreshTokenRepository();
        passwordHasher = new FakePasswordHasher();
        tokenIssuer = new FakeTokenIssuer();
        events = RecordingEventPublisher.create();

        registerHandler = new RegisterUserHandler(users, passwordHasher, events, FIXED_CLOCK);
        loginHandler = new LoginHandler(users, refreshTokens, passwordHasher, tokenIssuer, events, FIXED_CLOCK);
        refreshTokenHandler = new RefreshTokenHandler(users, refreshTokens, tokenIssuer, events, FIXED_CLOCK);
    }

    private UserView registerCustomer() {
        return registerHandler.handle(new RegisterUserCommand(EMAIL, PASSWORD, Set.of(Role.CUSTOMER)));
    }

    @Nested
    @DisplayName("registering")
    class Registering {

        @Test
        @DisplayName("given a new address, when registering, then the account is stored and announced")
        void registersANewAccount() {
            UserView view = registerCustomer();

            assertThat(view.email()).isEqualTo(EMAIL);
            assertThat(view.roles()).containsExactly(Role.CUSTOMER);
            assertThat(users.count()).isEqualTo(1);
            assertThat(events.eventsOfType(UserRegistered.class)).hasSize(1);
        }

        @Test
        @DisplayName("given no roles, when registering, then the account becomes a customer")
        void defaultsToCustomer() {
            UserView view = registerHandler.handle(new RegisterUserCommand(EMAIL, PASSWORD, Set.of()));

            assertThat(view.roles()).containsExactly(Role.CUSTOMER);
        }

        @Test
        @DisplayName("given ADMIN is requested, when registering, then it is refused")
        void refusesSelfAssignedAdmin() {
            assertThatThrownBy(() -> registerHandler.handle(
                    new RegisterUserCommand(EMAIL, PASSWORD, Set.of(Role.ADMIN))))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(errorCodeIs(ErrorCode.ACCESS_DENIED));

            assertThat(users.count()).isZero();
        }

        @Test
        @DisplayName("given the address is taken, when registering, then it is refused")
        void refusesDuplicateAddresses() {
            registerCustomer();

            assertThatThrownBy(() -> registerHandler.handle(
                    new RegisterUserCommand("ADA@NEO.IO", PASSWORD, Set.of(Role.CUSTOMER))))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .satisfies(errorCodeIs(ErrorCode.EMAIL_ALREADY_REGISTERED));
        }

        @Test
        @DisplayName("given a password, when registering, then what is stored is the hasher's output")
        void storesTheHashRatherThanThePassword() {
            registerCustomer();

            User stored = users.findByEmail(Email.of(EMAIL)).orElseThrow();

            assertThat(stored.passwordHash())
                    .isEqualTo(passwordHasher.hash(RawPassword.of(PASSWORD)));
            assertThat(stored.passwordHash().value()).isNotEqualTo(PASSWORD);
            assertThat(passwordHasher.matches(RawPassword.of(PASSWORD), stored.passwordHash())).isTrue();
            assertThat(passwordHasher.matches(RawPassword.of("a-different-password"),
                    stored.passwordHash())).isFalse();
        }

        @Test
        @DisplayName("given a user view, when serialised, then it never carries the hash")
        void theViewNeverExposesTheHash() {
            UserView view = registerCustomer();

            assertThat(view.toString()).doesNotContain("fake-bcrypt-hash-of");
            assertThat(UserView.class.getRecordComponents())
                    .extracting(java.lang.reflect.RecordComponent::getName)
                    .doesNotContain("passwordHash");
        }
    }

    @Nested
    @DisplayName("logging in")
    class LoggingIn {

        @BeforeEach
        void registerAnAccount() {
            registerCustomer();
            events.clear();
        }

        @Test
        @DisplayName("given the right password, when logging in, then a token pair is issued")
        void issuesTokens() {
            TokenPair tokens = loginHandler.handle(new LoginCommand(EMAIL, PASSWORD));

            assertThat(tokens.accessToken()).isNotBlank();
            assertThat(tokens.refreshToken()).isNotBlank();
            assertThat(tokens.tokenType()).isEqualTo("Bearer");
            assertThat(tokens.expiresIn()).isEqualTo(FakeTokenIssuer.ACCESS_TTL.toSeconds());
            assertThat(events.eventsOfType(AuthenticationEvents.UserLoggedIn.class)).hasSize(1);
        }

        @Test
        @DisplayName("given a successful login, when it completes, then the refresh token is recorded")
        void recordsTheRefreshTokenForLaterRevocation() {
            loginHandler.handle(new LoginCommand(EMAIL, PASSWORD));

            assertThat(refreshTokens.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("given a successful login, when it completes, then the last-login time is updated")
        void stampsTheLastLogin() {
            loginHandler.handle(new LoginCommand(EMAIL, PASSWORD));

            assertThat(users.findByEmail(Email.of(EMAIL)).orElseThrow().lastLoginAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("given a mixed-case address, when logging in, then it still matches the account")
        void ignoresAddressCasing() {
            assertThat(loginHandler.handle(new LoginCommand("ADA@Neo.IO", PASSWORD))).isNotNull();
        }

        @Test
        @DisplayName("given the wrong password, when logging in, then it is refused and recorded")
        void refusesTheWrongPassword() {
            assertThatThrownBy(() -> loginHandler.handle(new LoginCommand(EMAIL, "wrong-password-here")))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .satisfies(errorCodeIs(ErrorCode.INVALID_CREDENTIALS));

            assertThat(events.eventsOfType(AuthenticationEvents.LoginFailed.class)).hasSize(1);
        }

        @Test
        @DisplayName("given an unknown address, when logging in, then the error is identical")
        void revealsNothingAboutWhoExists() {
            DomainException unknownUser = catchDomainException(
                    () -> loginHandler.handle(new LoginCommand("nobody@neo.io", PASSWORD)));
            DomainException wrongPassword = catchDomainException(
                    () -> loginHandler.handle(new LoginCommand(EMAIL, "wrong-password-here")));

            assertThat(unknownUser.errorCode()).isEqualTo(wrongPassword.errorCode());
            assertThat(unknownUser.getMessage()).isEqualTo(wrongPassword.getMessage());
        }

        @Test
        @DisplayName("given an unknown address, when logging in, then a hash comparison still happens")
        void doesNotShortCircuitForUnknownAccounts() {
            passwordHasher.resetCallCount();

            assertThatThrownBy(() -> loginHandler.handle(new LoginCommand("nobody@neo.io", PASSWORD)))
                    .isInstanceOf(AuthenticationFailedException.class);

            assertThat(passwordHasher.matchCallCount())
                    .as("a decoy comparison keeps the timing indistinguishable")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("given a password too short to be real, when logging in, then it is still not fast-failed")
        void doesNotShortCircuitForUnusablePasswords() {
            passwordHasher.resetCallCount();

            assertThatThrownBy(() -> loginHandler.handle(new LoginCommand(EMAIL, "short")))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .satisfies(errorCodeIs(ErrorCode.INVALID_CREDENTIALS));

            assertThat(passwordHasher.matchCallCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("refreshing")
    class Refreshing {

        private TokenPair initial;

        @BeforeEach
        void logIn() {
            registerCustomer();
            initial = loginHandler.handle(new LoginCommand(EMAIL, PASSWORD));
            events.clear();
        }

        @Test
        @DisplayName("given a valid refresh token, when refreshed, then a different one comes back")
        void rotatesTheToken() {
            TokenPair rotated = refreshTokenHandler.handle(initial.refreshToken());

            assertThat(rotated.refreshToken()).isNotEqualTo(initial.refreshToken());
            assertThat(rotated.accessToken()).isNotBlank();
        }

        @Test
        @DisplayName("given a refresh, when it completes, then the presented token is marked spent")
        void burnsThePresentedToken() {
            UUID presentedId = tokenIssuer.decodeRefreshToken(initial.refreshToken()).tokenId();

            refreshTokenHandler.handle(initial.refreshToken());

            RefreshToken spent = refreshTokens.findByTokenId(presentedId).orElseThrow();
            assertThat(spent.isRevoked()).isTrue();
            assertThat(spent.replacedBy()).isNotNull();
        }

        @Test
        @DisplayName("given a rotated token, when the successor is used, then it works once")
        void theSuccessorIsUsable() {
            TokenPair rotated = refreshTokenHandler.handle(initial.refreshToken());

            assertThat(refreshTokenHandler.handle(rotated.refreshToken())).isNotNull();
        }

        @Test
        @DisplayName("given a spent token, when presented again, then reuse is reported")
        void detectsReplay() {
            refreshTokenHandler.handle(initial.refreshToken());

            assertThatThrownBy(() -> refreshTokenHandler.handle(initial.refreshToken()))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .satisfies(errorCodeIs(ErrorCode.REFRESH_TOKEN_REUSED));

            assertThat(events.eventsOfType(AuthenticationEvents.RefreshTokenReuseDetected.class))
                    .hasSize(1);
        }

        @Test
        @DisplayName("given replay is detected, when it is handled, then the whole family is revoked")
        void revokesTheWholeFamilyOnReplay() {
            TokenPair rotated = refreshTokenHandler.handle(initial.refreshToken());

            assertThatThrownBy(() -> refreshTokenHandler.handle(initial.refreshToken()))
                    .isInstanceOf(AuthenticationFailedException.class);

            assertThatThrownBy(() -> refreshTokenHandler.handle(rotated.refreshToken()))
                    .as("the successor a thief might also hold is dead too")
                    .isInstanceOf(AuthenticationFailedException.class);
        }

        @Test
        @DisplayName("given a token this service never issued, when refreshed, then it is refused")
        void refusesUnknownTokens() {
            String unknown = "refresh|" + UserId.newId() + '|' + UUID.randomUUID();

            assertThatThrownBy(() -> refreshTokenHandler.handle(unknown))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .satisfies(errorCodeIs(ErrorCode.INVALID_TOKEN));
        }

        @Test
        @DisplayName("given an access token, when presented as a refresh token, then it is refused")
        void refusesAnAccessTokenHere() {
            assertThatThrownBy(() -> refreshTokenHandler.handle(initial.accessToken()))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .satisfies(errorCodeIs(ErrorCode.INVALID_TOKEN));
        }

        @Test
        @DisplayName("given the token has expired, when refreshed, then it is refused")
        void refusesExpiredTokens() {
            Clock afterExpiry = com.neo.ticket.testsupport.TestFixtures.clockAt(
                    NOW.plus(FakeTokenIssuer.REFRESH_TTL).plusSeconds(1));
            RefreshTokenHandler later = new RefreshTokenHandler(
                    users, refreshTokens, tokenIssuer, events, afterExpiry);

            assertThatThrownBy(() -> later.handle(initial.refreshToken()))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .satisfies(errorCodeIs(ErrorCode.INVALID_TOKEN))
                    .hasMessageContaining("expired");
        }
    }

    private static DomainException catchDomainException(Runnable action) {
        try {
            action.run();
        } catch (DomainException expected) {
            return expected;
        }
        throw new AssertionError("expected a DomainException but none was thrown");
    }

    private static java.util.function.Consumer<Throwable> errorCodeIs(ErrorCode expected) {
        return thrown -> assertThat(((DomainException) thrown).errorCode()).isEqualTo(expected);
    }
}
