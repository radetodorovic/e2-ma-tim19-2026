package com.example.mobilnekt1.challenges.data;

import com.example.mobilnekt1.challenges.domain.Challenge;
import java.util.List;

public interface ChallengeListener {
    void onChanged(List<Challenge> challenges, String currentUserId);
    void onError(String message);
}
