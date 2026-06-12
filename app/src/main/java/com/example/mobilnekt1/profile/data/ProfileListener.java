package com.example.mobilnekt1.profile.data;

import com.example.mobilnekt1.profile.domain.PlayerStats;
import com.example.mobilnekt1.profile.domain.UserProfile;

public interface ProfileListener {
    void onChanged(UserProfile profile, PlayerStats stats);
    void onError(String message);
}
