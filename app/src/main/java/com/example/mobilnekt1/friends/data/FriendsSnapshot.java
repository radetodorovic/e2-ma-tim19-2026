package com.example.mobilnekt1.friends.data;

import com.example.mobilnekt1.friends.domain.FriendProfile;
import com.example.mobilnekt1.friends.domain.FriendRequest;
import com.example.mobilnekt1.friends.domain.MatchInvite;

import java.util.ArrayList;
import java.util.List;

public final class FriendsSnapshot {
    public final List<FriendProfile> friends;
    public final List<FriendRequest> incomingRequests;
    public final List<FriendRequest> outgoingRequests;
    public final List<MatchInvite> incomingInvites;
    public final List<MatchInvite> outgoingInvites;

    public FriendsSnapshot(List<FriendProfile> friends,
                           List<FriendRequest> incomingRequests,
                           List<FriendRequest> outgoingRequests,
                           List<MatchInvite> incomingInvites,
                           List<MatchInvite> outgoingInvites) {
        this.friends = new ArrayList<>(friends);
        this.incomingRequests = new ArrayList<>(incomingRequests);
        this.outgoingRequests = new ArrayList<>(outgoingRequests);
        this.incomingInvites = new ArrayList<>(incomingInvites);
        this.outgoingInvites = new ArrayList<>(outgoingInvites);
    }
}
