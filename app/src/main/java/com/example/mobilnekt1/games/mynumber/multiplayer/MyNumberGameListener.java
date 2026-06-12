package com.example.mobilnekt1.games.mynumber.multiplayer;

public interface MyNumberGameListener {
    void onChanged(MyNumberGameState state);

    void onError(String message);
}
