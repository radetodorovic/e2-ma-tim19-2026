package com.example.mobilnekt1.match.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class MatchGameSequence {
    public static final String QUIZ = "koZnaZna";
    public static final String CONNECTIONS = "spojnice";
    public static final String ASSOCIATIONS = "associations";
    public static final String SKOCKO = "skocko";
    public static final String STEP_BY_STEP = "stepByStep";
    public static final String MY_NUMBER = "myNumber";
    public static final String NONE = "none";

    public static final List<String> GAMES = Collections.unmodifiableList(Arrays.asList(
            QUIZ, CONNECTIONS, ASSOCIATIONS, SKOCKO, STEP_BY_STEP, MY_NUMBER));

    private MatchGameSequence() {
    }

    public static String firstGame() {
        return GAMES.get(0);
    }

    public static String nextGame(List<String> completedGames) {
        for (String game : GAMES) {
            if (completedGames == null || !completedGames.contains(game)) {
                return game;
            }
        }
        return NONE;
    }
}
