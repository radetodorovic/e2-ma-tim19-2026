package com.example.mobilnekt1.match.domain;

public final class MatchRewardCalculator {
    private static final long WIN_BASE_STARS = 10;
    private static final long POINTS_PER_STAR = 40;

    private MatchRewardCalculator() {
    }

    public static Result calculate(String player1Id, String player2Id,
                                   long player1Score, long player2Score,
                                   String matchType, String abandonedByUserId) {
        String winnerId;
        String loserId;
        if (abandonedByUserId != null) {
            loserId = abandonedByUserId;
            winnerId = abandonedByUserId.equals(player1Id) ? player2Id : player1Id;
        } else if (player1Score == player2Score) {
            winnerId = null;
            loserId = null;
        } else if (player1Score > player2Score) {
            winnerId = player1Id;
            loserId = player2Id;
        } else {
            winnerId = player2Id;
            loserId = player1Id;
        }

        long player1Delta = 0;
        long player2Delta = 0;
        if (MatchType.REGULAR.equals(matchType) && winnerId != null) {
            if (winnerId.equals(player1Id)) {
                player1Delta = winnerStars(player1Score);
                player2Delta = abandonedByUserId == null ? loserStars(player2Score) : 0;
            } else {
                player2Delta = winnerStars(player2Score);
                player1Delta = abandonedByUserId == null ? loserStars(player1Score) : 0;
            }
        }
        return new Result(winnerId, loserId, player1Delta, player2Delta);
    }

    public static long winnerStars(long score) {
        return WIN_BASE_STARS + scoreBonus(score);
    }

    public static long loserStars(long score) {
        return -WIN_BASE_STARS + scoreBonus(score);
    }

    private static long scoreBonus(long score) {
        return Math.max(0, score) / POINTS_PER_STAR;
    }

    public static final class Result {
        public final String winnerId;
        public final String loserId;
        public final long player1StarDelta;
        public final long player2StarDelta;

        Result(String winnerId, String loserId, long player1StarDelta, long player2StarDelta) {
            this.winnerId = winnerId;
            this.loserId = loserId;
            this.player1StarDelta = player1StarDelta;
            this.player2StarDelta = player2StarDelta;
        }
    }
}
