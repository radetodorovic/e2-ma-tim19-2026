package com.example.mobilnekt1.match.domain;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public final class MatchGameSequenceTest {
    @Test
    public void followsSpecificationOrder() {
        assertEquals(MatchGameSequence.QUIZ, MatchGameSequence.firstGame());
        assertEquals(MatchGameSequence.CONNECTIONS,
                MatchGameSequence.nextGame(Arrays.asList(MatchGameSequence.QUIZ)));
        assertEquals(MatchGameSequence.ASSOCIATIONS,
                MatchGameSequence.nextGame(Arrays.asList(
                        MatchGameSequence.QUIZ, MatchGameSequence.CONNECTIONS)));
        assertEquals(MatchGameSequence.SKOCKO,
                MatchGameSequence.nextGame(Arrays.asList(
                        MatchGameSequence.QUIZ, MatchGameSequence.CONNECTIONS,
                        MatchGameSequence.ASSOCIATIONS)));
        assertEquals(MatchGameSequence.STEP_BY_STEP,
                MatchGameSequence.nextGame(Arrays.asList(
                        MatchGameSequence.QUIZ, MatchGameSequence.CONNECTIONS,
                        MatchGameSequence.ASSOCIATIONS, MatchGameSequence.SKOCKO)));
        assertEquals(MatchGameSequence.MY_NUMBER,
                MatchGameSequence.nextGame(Arrays.asList(
                        MatchGameSequence.QUIZ, MatchGameSequence.CONNECTIONS,
                        MatchGameSequence.ASSOCIATIONS, MatchGameSequence.SKOCKO,
                        MatchGameSequence.STEP_BY_STEP)));
        assertEquals(MatchGameSequence.NONE,
                MatchGameSequence.nextGame(MatchGameSequence.GAMES));
    }
}
