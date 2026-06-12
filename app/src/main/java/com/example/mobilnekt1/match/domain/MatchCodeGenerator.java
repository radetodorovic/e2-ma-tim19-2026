package com.example.mobilnekt1.match.domain;

import java.security.SecureRandom;
import java.util.Locale;

public final class MatchCodeGenerator {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random;

    public MatchCodeGenerator() {
        this(new SecureRandom());
    }

    MatchCodeGenerator(SecureRandom random) {
        this.random = random;
    }

    public String generate() {
        StringBuilder result = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            result.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return result.toString();
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String value) {
        String normalized = normalize(value);
        if (normalized.length() != CODE_LENGTH) {
            return false;
        }
        for (int i = 0; i < normalized.length(); i++) {
            if (ALPHABET.indexOf(normalized.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }
}
