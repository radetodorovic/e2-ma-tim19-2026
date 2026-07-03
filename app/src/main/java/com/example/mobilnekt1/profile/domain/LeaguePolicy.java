package com.example.mobilnekt1.profile.domain;

public final class LeaguePolicy {
    public static final int MIN_LEAGUE = 0;
    public static final int MAX_LEAGUE = 5;

    private LeaguePolicy() {
    }

    public static int leagueForStars(long stars) {
        long safeStars = Math.max(0, stars);
        int league = MIN_LEAGUE;
        long threshold = 100;
        while (league < MAX_LEAGUE && safeStars >= threshold) {
            league++;
            threshold *= 2;
        }
        return league;
    }

    public static long applyMonthlyNonPlacementPenalty(long stars) {
        long safeStars = Math.max(0, stars);
        return safeStars - safeStars * 30 / 100;
    }
}
