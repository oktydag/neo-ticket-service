package com.neo.ticket.iam.infrastructure.security;

public final class JwtClaims {

    public static final String ROLES = "roles";

    public static final String TOKEN_TYPE = "typ";

    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";

    private JwtClaims() {
    }
}
