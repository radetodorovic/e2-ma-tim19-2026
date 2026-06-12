package com.example.mobilnekt1.match.data;

import com.example.mobilnekt1.match.domain.Match;

public interface MatchListener {
    void onChanged(Match match);

    void onError(String message);
}
