package com.example.mobilnekt1.missions.data;

import com.example.mobilnekt1.missions.domain.DailyMissions;

public interface DailyMissionsListener {
    void onChanged(DailyMissions missions);
    void onError(String message);
}
