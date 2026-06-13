package com.example.mobilnekt1.games.quiz.presentation;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobilnekt1.auth.presentation.Event;
import com.example.mobilnekt1.games.quiz.data.QuizGameListener;
import com.example.mobilnekt1.games.quiz.data.QuizGameRepository;
import com.example.mobilnekt1.games.quiz.domain.QuizGameState;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.profile.data.StatsRepository;
import com.example.mobilnekt1.match.data.MatchScoreRepository;

public final class QuizGameViewModel extends AndroidViewModel {
    private final QuizGameRepository repository;
    private final StatsRepository statsRepository;
    private final MatchScoreRepository matchScoreRepository;
    private final MutableLiveData<QuizGameState> state = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> error = new MutableLiveData<>();
    private String matchId;
    private boolean resolving;
    private boolean statsCommitted;

    public QuizGameViewModel(@NonNull Application application) {
        super(application);
        repository = new QuizGameRepository(application);
        statsRepository = new StatsRepository(application);
        matchScoreRepository = new MatchScoreRepository(application);
    }

    public LiveData<QuizGameState> getState() { return state; }
    public LiveData<Event<String>> getError() { return error; }
    public String currentUserId() { return repository.currentUserId(); }

    public void start(String id) {
        if (matchId != null) return;
        matchId = id;
        repository.listen(id, new QuizGameListener() {
            @Override public void onChanged(QuizGameState value) {
                state.setValue(value);
                resolving = false;
                if (!value.isFinished() && value.player1Submitted && value.player2Submitted) resolve();
                if (value.isFinished() && !statsCommitted) {
                    statsCommitted = true;
                    statsRepository.commitQuiz(matchId, callback());
                    matchScoreRepository.commitGameResult(matchId, "koZnaZna", "status", callback());
                }
            }
            @Override public void onError(String message) { showError(message); }
        });
        repository.initialize(id, callback());
    }

    public void answer(int index) {
        if (matchId != null) repository.submitAnswer(matchId, index, callback());
    }

    public void resolveIfExpired() { resolve(); }

    private void resolve() {
        if (matchId == null || resolving) return;
        resolving = true;
        repository.resolveQuestion(matchId, new GameActionCallback() {
            @Override public void onSuccess() { resolving = false; }
            @Override public void onError(String message) { resolving = false; showError(message); }
        });
    }

    private GameActionCallback callback() {
        return new GameActionCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String message) { showError(message); }
        };
    }

    private void showError(String message) { error.setValue(new Event<>(message)); }

    @Override protected void onCleared() {
        repository.stopListening();
        super.onCleared();
    }
}
