package com.example.mobilnekt1.ranking.data;

import com.example.mobilnekt1.ranking.domain.RankingEntry;
import java.util.List;

public interface RankingListener {
    void onChanged(List<RankingEntry> weekly, List<RankingEntry> monthly);
    void onError(String message);
}
