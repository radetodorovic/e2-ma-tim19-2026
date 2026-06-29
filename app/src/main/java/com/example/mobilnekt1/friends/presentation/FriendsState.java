package com.example.mobilnekt1.friends.presentation;

import com.example.mobilnekt1.friends.data.FriendsSnapshot;
import com.example.mobilnekt1.friends.domain.FriendProfile;

import java.util.ArrayList;

public final class FriendsState {
    public final FriendsSnapshot snapshot;
    public final FriendProfile searchResult;
    public final boolean loading;

    public FriendsState(FriendsSnapshot snapshot, FriendProfile searchResult, boolean loading) {
        this.snapshot = snapshot;
        this.searchResult = searchResult;
        this.loading = loading;
    }

    public static FriendsState empty() {
        return new FriendsState(new FriendsSnapshot(new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()), null, false);
    }
}
