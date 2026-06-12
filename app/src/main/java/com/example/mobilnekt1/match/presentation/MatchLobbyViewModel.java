package com.example.mobilnekt1.match.presentation;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobilnekt1.auth.presentation.Event;
import com.example.mobilnekt1.match.data.MatchCallback;
import com.example.mobilnekt1.match.data.MatchListener;
import com.example.mobilnekt1.match.data.MatchRepository;
import com.example.mobilnekt1.match.domain.Match;

public final class MatchLobbyViewModel extends AndroidViewModel {
    private final MatchRepository repository;
    private final MutableLiveData<MatchLobbyState> state =
            new MutableLiveData<>(MatchLobbyState.idle());
    private final MutableLiveData<Event<String>> openGame = new MutableLiveData<>();
    private String matchId;
    private long lastObservedGameVersion;

    public MatchLobbyViewModel(@NonNull Application application) {
        super(application);
        repository = new MatchRepository(application);
    }

    public LiveData<MatchLobbyState> getState() {
        return state;
    }

    public LiveData<Event<String>> getOpenGame() {
        return openGame;
    }

    public void createMatch() {
        setLoading();
        repository.createMatch(operationCallback());
    }

    public void joinMatch(String code) {
        setLoading();
        repository.joinMatch(code, operationCallback());
    }

    public void selectGame(String game) {
        if (matchId == null) {
            showError("Prvo kreirajte partiju ili se pridruzite postojecoj.");
            return;
        }
        setLoading();
        repository.selectGame(matchId, game, operationCallback());
    }

    public void clearError() {
        MatchLobbyState current = state.getValue();
        if (current != null && current.error != null) {
            state.setValue(MatchLobbyState.match(current.match));
        }
    }

    private MatchCallback operationCallback() {
        return new MatchCallback() {
            @Override
            public void onSuccess(String resultMatchId) {
                matchId = resultMatchId;
                if (state.getValue() == null || state.getValue().match == null) {
                    observeMatch(resultMatchId);
                } else {
                    state.setValue(MatchLobbyState.match(state.getValue().match));
                }
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        };
    }

    private void observeMatch(String id) {
        repository.listen(id, new MatchListener() {
            @Override
            public void onChanged(Match match) {
                state.setValue(MatchLobbyState.match(match));
                if (match.currentGame != null && !"none".equals(match.currentGame)
                        && match.currentGameVersion > lastObservedGameVersion) {
                    lastObservedGameVersion = match.currentGameVersion;
                    openGame.setValue(new Event<>(match.currentGame));
                }
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void setLoading() {
        MatchLobbyState current = state.getValue();
        state.setValue(MatchLobbyState.loading(current == null ? null : current.match));
    }

    private void showError(String message) {
        MatchLobbyState current = state.getValue();
        state.setValue(MatchLobbyState.error(current == null ? null : current.match, message));
    }

    @Override
    protected void onCleared() {
        repository.stopListening();
        super.onCleared();
    }
}
