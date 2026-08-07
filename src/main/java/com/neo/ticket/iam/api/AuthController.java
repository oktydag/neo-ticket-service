package com.neo.ticket.iam.api;

import com.neo.ticket.iam.api.dto.LoginRequest;
import com.neo.ticket.iam.api.dto.RefreshRequest;
import com.neo.ticket.iam.api.dto.RegisterRequest;
import com.neo.ticket.iam.application.TokenPair;
import com.neo.ticket.iam.application.UserView;
import com.neo.ticket.iam.application.command.LoginCommand;
import com.neo.ticket.iam.application.command.RegisterUserCommand;
import com.neo.ticket.iam.application.command.handlers.LoginHandler;
import com.neo.ticket.iam.application.command.handlers.RefreshTokenHandler;
import com.neo.ticket.iam.application.command.handlers.RegisterUserHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration and token issuance")
@SecurityRequirements
class AuthController {

    private final RegisterUserHandler registerUserHandler;
    private final LoginHandler loginHandler;
    private final RefreshTokenHandler refreshTokenHandler;

    AuthController(RegisterUserHandler registerUserHandler,
                   LoginHandler loginHandler,
                   RefreshTokenHandler refreshTokenHandler) {
        this.registerUserHandler = registerUserHandler;
        this.loginHandler = loginHandler;
        this.refreshTokenHandler = refreshTokenHandler;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new account",
            description = "Grants CUSTOMER when no roles are requested. ADMIN cannot be self-assigned.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "ADMIN was requested",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "E-mail already registered",
                    content = @Content)
    })
    ResponseEntity<UserView> register(@Valid @RequestBody RegisterRequest request) {
        UserView created = registerUserHandler.handle(new RegisterUserCommand(
                request.email(), request.password(), request.requestedRoles()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange credentials for tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access and refresh tokens issued"),
            @ApiResponse(responseCode = "401", description = "E-mail or password is wrong",
                    content = @Content),
            @ApiResponse(responseCode = "429", description = "Too many attempts",
                    content = @Content)
    })
    TokenPair login(@Valid @RequestBody LoginRequest request) {
        return loginHandler.handle(new LoginCommand(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new pair",
            description = """
                    Refresh tokens are single-use. The token presented is invalidated and a
                    new one returned. Presenting an already-spent token revokes every token
                    descended from the same login, on the assumption that it was stolen.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A new token pair"),
            @ApiResponse(responseCode = "401",
                    description = "Token invalid, expired or already spent", content = @Content)
    })
    TokenPair refresh(@Valid @RequestBody RefreshRequest request) {
        return refreshTokenHandler.handle(request.refreshToken());
    }
}
