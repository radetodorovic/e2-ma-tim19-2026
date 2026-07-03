package com.example.mobilnekt1.challenges.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public final class Challenge {
    public String id;
    public String creatorId;
    public String creatorName;
    public String status;
    public long starStake;
    public long tokenStake;
    public List<String> participantIds = new ArrayList<>();
    public Map<String, String> participantNames = new HashMap<>();
    public Map<String, Long> participantScores = new HashMap<>();
    public List<String> claimedIds = new ArrayList<>();
}
