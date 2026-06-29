package com.example.mobilnekt1.match.domain;

public final class StarTokenProgress {
    public static final long STARS_PER_TOKEN = 50;

    private StarTokenProgress() {
    }

    public static Result apply(long currentProgress, long starDelta) {
        long earnedStars = Math.max(0, starDelta);
        long totalProgress = Math.max(0, currentProgress) + earnedStars;
        return new Result(totalProgress / STARS_PER_TOKEN,
                totalProgress % STARS_PER_TOKEN);
    }

    public static final class Result {
        public final long earnedTokens;
        public final long remainingProgress;

        Result(long earnedTokens, long remainingProgress) {
            this.earnedTokens = earnedTokens;
            this.remainingProgress = remainingProgress;
        }
    }
}
