package com.example.mobilnekt1.friends.data;

import com.example.mobilnekt1.friends.domain.FriendProfile;

public interface FriendSearchCallback {
    void onSuccess(FriendProfile profile);

    void onError(String message);
}
