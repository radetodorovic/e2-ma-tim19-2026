package com.example.mobilnekt1.profile.domain;

import java.util.HashMap;
import java.util.Map;

public final class PlayerStats {
    public long totalMatches;
    public long wins;
    public long losses;
    public long koZnaZnaCorrect;
    public long koZnaZnaWrong;
    public long spojniceCorrectPairs;
    public long spojniceTotalPairs;
    public long myNumberExactRounds;
    public long myNumberTotalRounds;
    public long stepRoundsPlayed;
    public long associationsSolved;
    public long associationsTotal;
    public long skockoRoundsPlayed;
    public Map<String, Long> skockoSolvedByAttempt = new HashMap<>();
    public Map<String, Long> stepSolvedByHint = new HashMap<>();
    public Map<String, Double> averageScoreByGame = new HashMap<>();
    public Map<String, Long> gamesPlayedByGame = new HashMap<>();

    public PlayerStats() {
    }
}
