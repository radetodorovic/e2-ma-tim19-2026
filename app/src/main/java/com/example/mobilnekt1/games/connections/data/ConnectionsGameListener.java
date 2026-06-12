package com.example.mobilnekt1.games.connections.data;

import com.example.mobilnekt1.games.connections.domain.ConnectionsGameState;

public interface ConnectionsGameListener {
    void onChanged(ConnectionsGameState state);
    void onError(String message);
}
