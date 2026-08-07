package com.neo.ticket.shared.domain.valueobject;

public enum Role {

    ADMIN,
    ORGANIZER,
    CUSTOMER;

    public static final String AUTHORITY_PREFIX = "ROLE_";

    public String authority() {
        return AUTHORITY_PREFIX + name();
    }

    public static Role fromAuthority(String authority) {
        String name = authority.startsWith(AUTHORITY_PREFIX)
                ? authority.substring(AUTHORITY_PREFIX.length())
                : authority;
        return Role.valueOf(name);
    }
}
