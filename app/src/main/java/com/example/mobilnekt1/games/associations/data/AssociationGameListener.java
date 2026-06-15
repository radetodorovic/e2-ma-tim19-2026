package com.example.mobilnekt1.games.associations.data;

import com.example.mobilnekt1.games.associations.domain.AssociationGameState;

public interface AssociationGameListener {
    void onChanged(AssociationGameState state);
    void onError(String message);
}
