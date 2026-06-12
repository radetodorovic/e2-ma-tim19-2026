package com.example.mobilnekt1.games.stepbystep.multiplayer;

public interface StepGameListener {
    void onChanged(StepGameState state);

    void onError(String message);
}
