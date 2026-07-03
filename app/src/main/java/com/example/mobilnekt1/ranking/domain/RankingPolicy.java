package com.example.mobilnekt1.ranking.domain;

public final class RankingPolicy {
    public enum Period { WEEKLY, MONTHLY }

    private RankingPolicy() { }

    public static int tokenReward(Period period, int position) {
        if (position < 1 || position > 10) return 0;
        int weekly = position == 1 ? 5 : position == 2 ? 3 : position == 3 ? 2 : 1;
        return period == Period.MONTHLY ? weekly * 2 : weekly;
    }
}
