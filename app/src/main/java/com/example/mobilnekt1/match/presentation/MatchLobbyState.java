package com.example.mobilnekt1.match.presentation;

import com.example.mobilnekt1.match.domain.Match;

public final class MatchLobbyState {
    public final boolean loading;
    public final Match match;
    public final String error;

    private MatchLobbyState(boolean loading, Match match, String error) {
        this.loading = loading;
        this.match = match;
        this.error = error;
    }

    public static MatchLobbyState idle() {
        return new MatchLobbyState(false, null, null);
    }

    public static MatchLobbyState loading(Match currentMatch) {
        return new MatchLobbyState(true, currentMatch, null);
    }

    public static MatchLobbyState match(Match match) {
        return new MatchLobbyState(false, match, null);
    }

    public static MatchLobbyState error(Match currentMatch, String error) {
        return new MatchLobbyState(false, currentMatch, error);
    }
}
