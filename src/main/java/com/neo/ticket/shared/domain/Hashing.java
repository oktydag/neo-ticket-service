package com.neo.ticket.shared.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Hashing {

    private static final String ALGORITHM = "SHA-256";

    public static final int SHA256_HEX_LENGTH = 64;

    private Hashing() {
    }

    public static String sha256Hex(String input) {
        Invariants.requirePresent(input, "input");
        try {
            byte[] digest = MessageDigest.getInstance(ALGORITHM)
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException cause) {
            throw new IllegalStateException("%s is unavailable".formatted(ALGORITHM), cause);
        }
    }
}
