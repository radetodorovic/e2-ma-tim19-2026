package com.example.mobilnekt1.games.associations;

import static org.junit.Assert.assertEquals;

import com.example.mobilnekt1.games.associations.domain.AssociationScoringEngine;
import org.junit.Test;

public class AssociationScoringEngineTest {
    @Test public void columnRewardsUnopenedFields() {
        assertEquals(6, AssociationScoringEngine.columnPoints(0));
        assertEquals(5, AssociationScoringEngine.columnPoints(1));
        assertEquals(2, AssociationScoringEngine.columnPoints(4));
    }

    @Test public void finalIncludesOnlyPreviouslyUnsolvedColumns() {
        assertEquals(30, AssociationScoringEngine.finalPoints(
                new int[]{1, 0, 0, 0}, new boolean[]{false, false, false, false}));
        assertEquals(25, AssociationScoringEngine.finalPoints(
                new int[]{4, 0, 0, 0}, new boolean[]{true, false, false, false}));
    }
}
