package com.example.mobilnekt1.games.connections.presentation;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.mobilnekt1.auth.presentation.Event;
import com.example.mobilnekt1.games.connections.data.ConnectionsGameListener;
import com.example.mobilnekt1.games.connections.data.ConnectionsGameRepository;
import com.example.mobilnekt1.games.connections.domain.ConnectionsGameState;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.profile.data.StatsRepository;
import com.example.mobilnekt1.match.data.MatchScoreRepository;

public final class ConnectionsGameViewModel extends AndroidViewModel {
    private final ConnectionsGameRepository repository;
    private final StatsRepository statsRepository;
    private final MatchScoreRepository matchScoreRepository;
    private final MutableLiveData<ConnectionsGameState> state = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> error = new MutableLiveData<>();
    private String matchId;
    private boolean advancing;
    private boolean statsCommitted;

    public ConnectionsGameViewModel(@NonNull Application app) {
        super(app);
        repository = new ConnectionsGameRepository(app);
        statsRepository = new StatsRepository(app);
        matchScoreRepository = new MatchScoreRepository(app);
    }
    public LiveData<ConnectionsGameState> getState() { return state; }
    public LiveData<Event<String>> getError() { return error; }
    public String currentUserId() { return repository.currentUserId(); }
    public void start(String id) {
        if (matchId != null) return;
        matchId = id;
        repository.listen(id, new ConnectionsGameListener() {
            @Override public void onChanged(ConnectionsGameState value) {
                state.setValue(value); advancing = false;
                if (value.isFinished() && !statsCommitted) {
                    statsCommitted = true;
                    statsRepository.commitConnections(matchId, callback());
                    matchScoreRepository.commitGameResult(matchId, "spojnice", "phase", callback());
                }
            }
            @Override public void onError(String message) { showError(message); }
        });
        repository.initialize(id, callback());
    }
    public void choose(int rightIndex) { if (matchId != null) repository.choose(matchId, rightIndex, callback()); }
    public void advanceExpired() {
        if (matchId == null || advancing) return;
        advancing = true;
        repository.advanceExpired(matchId, new GameActionCallback() {
            @Override public void onSuccess() { advancing = false; }
            @Override public void onError(String message) { advancing = false; showError(message); }
        });
    }
    private GameActionCallback callback() { return new GameActionCallback() {
        @Override public void onSuccess() { }
        @Override public void onError(String message) { showError(message); }
    }; }
    private void showError(String message) { error.setValue(new Event<>(message)); }
    @Override protected void onCleared() { repository.stopListening(); super.onCleared(); }
}
