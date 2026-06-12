package com.example.mobilnekt1.profile.presentation;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.mobilnekt1.auth.data.FirebaseAuthRepository;
import com.example.mobilnekt1.auth.presentation.Event;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.profile.data.ProfileListener;
import com.example.mobilnekt1.profile.data.ProfileRepository;
import com.example.mobilnekt1.profile.domain.PlayerStats;
import com.example.mobilnekt1.profile.domain.UserProfile;

public final class ProfileViewModel extends AndroidViewModel {
    public static final class State {
        public final UserProfile profile; public final PlayerStats stats;
        public State(UserProfile profile, PlayerStats stats) { this.profile = profile; this.stats = stats; }
    }
    private final ProfileRepository repository;
    private final FirebaseAuthRepository auth;
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> error = new MutableLiveData<>();
    public ProfileViewModel(@NonNull Application app) {
        super(app); repository = new ProfileRepository(app); auth = FirebaseAuthRepository.getInstance(app);
        repository.listen(new ProfileListener() {
            @Override public void onChanged(UserProfile profile, PlayerStats stats) { state.setValue(new State(profile, stats)); }
            @Override public void onError(String message) { error.setValue(new Event<>(message)); }
        });
    }
    public LiveData<State> getState() { return state; }
    public LiveData<Event<String>> getError() { return error; }
    public void updateAvatar(String id) { repository.updateAvatar(id, new GameActionCallback() {
        @Override public void onSuccess() { }
        @Override public void onError(String message) { error.setValue(new Event<>(message)); }
    }); }
    public void logout() { auth.logout(); }
    @Override protected void onCleared() { repository.stop(); super.onCleared(); }
}
