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
    public Map<String, Double> averageScoreByGame = new HashMap<>();
    public Map<String, Long> gamesPlayedByGame = new HashMap<>();

    public PlayerStats() {
    }
}
