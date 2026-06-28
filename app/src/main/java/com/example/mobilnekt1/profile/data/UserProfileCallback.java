package com.example.mobilnekt1.profile.data;

import com.example.mobilnekt1.profile.domain.UserProfile;

public interface UserProfileCallback {
    void onSuccess(UserProfile profile);

    void onError(String message);
}
