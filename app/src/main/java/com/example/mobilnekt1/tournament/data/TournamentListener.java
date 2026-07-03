package com.example.mobilnekt1.tournament.data;
import com.example.mobilnekt1.tournament.domain.Tournament;
import java.util.List;
public interface TournamentListener {
    void onChanged(List<Tournament> tournaments, String currentUserId);
    void onError(String message);
}
