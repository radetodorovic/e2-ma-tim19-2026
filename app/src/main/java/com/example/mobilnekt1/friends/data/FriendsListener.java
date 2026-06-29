package com.example.mobilnekt1.friends.data;

public interface FriendsListener {
    void onChanged(FriendsSnapshot snapshot);

    void onError(String message);
}
