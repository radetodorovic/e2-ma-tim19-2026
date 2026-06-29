package com.example.mobilnekt1.match.domain;

public final class MatchType {
    public static final String REGULAR = "regular";
    public static final String FRIENDLY = "friendly";
    public static final String TOURNAMENT = "tournament";
    public static final String CHALLENGE = "challenge";

    private MatchType() {
    }

    public static boolean isSupported(String value) {
        return REGULAR.equals(value) || FRIENDLY.equals(value)
                || TOURNAMENT.equals(value) || CHALLENGE.equals(value);
    }
}
