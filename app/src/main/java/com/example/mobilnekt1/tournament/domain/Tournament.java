package com.example.mobilnekt1.tournament.domain;
import java.util.*;
public final class Tournament {
    public String id, status, creatorId;
    public List<String> participantIds = new ArrayList<>();
    public Map<String, String> participantNames = new HashMap<>();
    public List<String> semifinalOneIds = new ArrayList<>();
    public List<String> semifinalTwoIds = new ArrayList<>();
    public String semifinalOneMatchId, semifinalTwoMatchId, semifinalOneWinnerId,
            semifinalTwoWinnerId, finalMatchId, winnerId;
}
