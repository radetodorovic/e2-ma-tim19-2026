package com.example.mobilnekt1.profile.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LeaguePolicyTest {
    @Test
    public void leagueForStars_usesSpecificationThresholds() {
        assertEquals(0, LeaguePolicy.leagueForStars(99));
        assertEquals(1, LeaguePolicy.leagueForStars(100));
        assertEquals(2, LeaguePolicy.leagueForStars(200));
        assertEquals(3, LeaguePolicy.leagueForStars(400));
        assertEquals(4, LeaguePolicy.leagueForStars(800));
        assertEquals(5, LeaguePolicy.leagueForStars(1600));
        assertEquals(5, LeaguePolicy.leagueForStars(100_000));
    }

    @Test
    public void leagueForStars_handlesLossAndNegativeValues() {
        assertEquals(0, LeaguePolicy.leagueForStars(-10));
        assertEquals(0, LeaguePolicy.leagueForStars(95));
        assertEquals(1, LeaguePolicy.leagueForStars(190));
    }

    @Test
    public void monthlyPenalty_removesThirtyPercentWithoutGoingNegative() {
        assertEquals(301, LeaguePolicy.applyMonthlyNonPlacementPenalty(430));
        assertEquals(0, LeaguePolicy.applyMonthlyNonPlacementPenalty(-5));
    }
}
