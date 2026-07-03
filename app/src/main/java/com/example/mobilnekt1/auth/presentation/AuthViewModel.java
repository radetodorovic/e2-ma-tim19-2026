package com.example.mobilnekt1.auth.presentation;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobilnekt1.auth.data.AuthCallback;
import com.example.mobilnekt1.auth.data.FirebaseAuthRepository;

public final class AuthViewModel extends AndroidViewModel {
    private final FirebaseAuthRepository repository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Event<AuthResult>> result = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = FirebaseAuthRepository.getInstance(application);
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Event<AuthResult>> getResult() {
        return result;
    }

    public void register(String email, String username, String region, String password) {
        execute(callback -> repository.register(email, username, region, password, callback));
    }

    public void login(String identifier, String password) {
        execute(callback -> repository.login(identifier, password, callback));
    }

    public void loginAsGuest() {
        execute(repository::loginAsGuest);
    }

    public void checkEmailVerification() {
        execute(repository::checkEmailVerification);
    }

    public void resendVerification() {
        execute(repository::resendVerification);
    }

    public void sendPasswordReset(String email) {
        execute(callback -> repository.sendPasswordReset(email, callback));
    }

    public void changePassword(String oldPassword, String newPassword) {
        execute(callback -> repository.changePassword(oldPassword, newPassword, callback));
    }

    public boolean hasVerifiedUser() {
        return repository.hasVerifiedUser();
    }

    public boolean isGuest() {
        return repository.isGuest();
    }

    public boolean hasUserAwaitingVerification() {
        return repository.hasUserAwaitingVerification();
    }

    public void logout() {
        repository.logout();
    }

    private void execute(Operation operation) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        loading.setValue(true);
        operation.run(new AuthCallback() {
            @Override
            public void onSuccess() {
                finish(AuthResult.success());
            }

            @Override
            public void onVerificationRequired() {
                finish(AuthResult.verificationRequired());
            }

            @Override
            public void onError(String message) {
                finish(AuthResult.error(message));
            }
        });
    }

    private void finish(AuthResult authResult) {
        loading.setValue(false);
        result.setValue(new Event<>(authResult));
    }

    private interface Operation {
        void run(AuthCallback callback);
    }
}
