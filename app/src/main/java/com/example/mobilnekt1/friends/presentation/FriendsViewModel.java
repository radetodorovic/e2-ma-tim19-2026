package com.example.mobilnekt1.friends.presentation;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobilnekt1.auth.presentation.Event;
import com.example.mobilnekt1.friends.data.FriendSearchCallback;
import com.example.mobilnekt1.friends.data.FriendsListener;
import com.example.mobilnekt1.friends.data.FriendsRepository;
import com.example.mobilnekt1.friends.data.FriendsSnapshot;
import com.example.mobilnekt1.friends.data.MatchInviteCallback;
import com.example.mobilnekt1.friends.domain.FriendProfile;
import com.example.mobilnekt1.games.shared.GameActionCallback;

public final class FriendsViewModel extends AndroidViewModel {
    private final FriendsRepository repository;
    private final Handler timer = new Handler(Looper.getMainLooper());
    private final MutableLiveData<FriendsState> state =
            new MutableLiveData<>(FriendsState.empty());
    private final MutableLiveData<Event<String>> message = new MutableLiveData<>();
    private final MutableLiveData<Event<OpenFriendlyMatch>> openMatch = new MutableLiveData<>();
    private final Runnable expiryTick = new Runnable() {
        @Override public void run() {
            repository.expirePendingInvites();
            timer.postDelayed(this, 500);
        }
    };

    public FriendsViewModel(@NonNull Application application) {
        super(application);
        repository = new FriendsRepository(application);
        repository.listen(new FriendsListener() {
            @Override public void onChanged(FriendsSnapshot snapshot) {
                FriendsState current = current();
                state.setValue(new FriendsState(snapshot, current.searchResult, false));
            }

            @Override public void onError(String value) { showMessage(value); }
        });
        timer.post(expiryTick);
    }

    public LiveData<FriendsState> getState() { return state; }
    public LiveData<Event<String>> getMessage() { return message; }
    public LiveData<Event<OpenFriendlyMatch>> getOpenMatch() { return openMatch; }

    public void search(String username) {
        setLoading(true);
        repository.searchByUsername(username, searchCallback());
    }

    public void handleQr(String value) {
        setLoading(true);
        repository.findByQrValue(value, searchCallback());
    }

    public void clearSearch() {
        FriendsState current = current();
        state.setValue(new FriendsState(current.snapshot, null, false));
    }

    public void sendRequest(String userId) {
        repository.sendFriendRequest(userId, action("Zahtev za prijateljstvo je poslat."));
    }

    public void acceptRequest(String requestId) {
        repository.acceptFriendRequest(requestId, action("Zahtev je prihvacen."));
    }

    public void rejectRequest(String requestId) {
        repository.rejectFriendRequest(requestId, action("Zahtev je odbijen."));
    }

    public void cancelRequest(String requestId) {
        repository.cancelFriendRequest(requestId, action("Zahtev je otkazan."));
    }

    public void invite(String friendId) {
        setLoading(true);
        repository.sendFriendlyInvite(friendId, matchCallback());
    }

    public void acceptInvite(String inviteId) {
        setLoading(true);
        repository.acceptInvite(inviteId, matchCallback());
    }

    public void rejectInvite(String inviteId) {
        repository.rejectInvite(inviteId, action("Poziv je odbijen."));
    }

    public void cancelInvite(String inviteId) {
        repository.cancelInvite(inviteId, action("Poziv je otkazan."));
    }

    private FriendSearchCallback searchCallback() {
        return new FriendSearchCallback() {
            @Override public void onSuccess(FriendProfile profile) {
                state.setValue(new FriendsState(current().snapshot, profile, false));
            }

            @Override public void onError(String value) {
                setLoading(false);
                showMessage(value);
            }
        };
    }

    private MatchInviteCallback matchCallback() {
        return new MatchInviteCallback() {
            @Override public void onSuccess(String matchId, String inviteId) {
                setLoading(false);
                openMatch.setValue(new Event<>(new OpenFriendlyMatch(matchId, inviteId)));
            }

            @Override public void onError(String value) {
                setLoading(false);
                showMessage(value);
            }
        };
    }

    private GameActionCallback action(String successMessage) {
        setLoading(true);
        return new GameActionCallback() {
            @Override public void onSuccess() {
                setLoading(false);
                showMessage(successMessage);
            }

            @Override public void onError(String value) {
                setLoading(false);
                showMessage(value);
            }
        };
    }

    private FriendsState current() {
        FriendsState value = state.getValue();
        return value == null ? FriendsState.empty() : value;
    }

    private void setLoading(boolean loading) {
        FriendsState current = current();
        state.setValue(new FriendsState(current.snapshot, current.searchResult, loading));
    }

    private void showMessage(String value) { message.setValue(new Event<>(value)); }

    @Override protected void onCleared() {
        timer.removeCallbacksAndMessages(null);
        repository.stop();
        super.onCleared();
    }
}
