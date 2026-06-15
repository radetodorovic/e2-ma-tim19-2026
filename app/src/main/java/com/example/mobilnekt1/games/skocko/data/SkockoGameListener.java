package com.example.mobilnekt1.games.skocko.data;

import com.example.mobilnekt1.games.skocko.domain.SkockoGameState;

public interface SkockoGameListener {
    void onChanged(SkockoGameState state);
    void onError(String message);
}
