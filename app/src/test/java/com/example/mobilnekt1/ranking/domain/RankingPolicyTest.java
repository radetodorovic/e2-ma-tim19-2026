package com.example.mobilnekt1.ranking.domain;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RankingPolicyTest {
    @Test public void weeklyRewards_followSpecification() {
        assertEquals(5, RankingPolicy.tokenReward(RankingPolicy.Period.WEEKLY, 1));
        assertEquals(3, RankingPolicy.tokenReward(RankingPolicy.Period.WEEKLY, 2));
        assertEquals(2, RankingPolicy.tokenReward(RankingPolicy.Period.WEEKLY, 3));
        assertEquals(1, RankingPolicy.tokenReward(RankingPolicy.Period.WEEKLY, 10));
        assertEquals(0, RankingPolicy.tokenReward(RankingPolicy.Period.WEEKLY, 11));
    }
    @Test public void monthlyRewards_areDoubleWeeklyRewards() {
        assertEquals(10, RankingPolicy.tokenReward(RankingPolicy.Period.MONTHLY, 1));
        assertEquals(6, RankingPolicy.tokenReward(RankingPolicy.Period.MONTHLY, 2));
        assertEquals(4, RankingPolicy.tokenReward(RankingPolicy.Period.MONTHLY, 3));
        assertEquals(2, RankingPolicy.tokenReward(RankingPolicy.Period.MONTHLY, 10));
    }
}
