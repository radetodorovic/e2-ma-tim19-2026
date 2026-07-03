package com.example.mobilnekt1.regions.data;

import com.example.mobilnekt1.regions.domain.RegionStats;
import com.example.mobilnekt1.regions.domain.RegionPlayerPoint;
import java.util.List;

public interface RegionsListener {
    void onChanged(List<RegionStats> regions, List<RegionPlayerPoint> points, String currentRegion);
    void onError(String message);
}
