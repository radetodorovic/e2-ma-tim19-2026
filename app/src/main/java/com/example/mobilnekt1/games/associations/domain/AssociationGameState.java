package com.example.mobilnekt1.games.associations.domain;

import java.util.ArrayList;
import java.util.List;

public final class AssociationGameState {
    public String player1Id;
    public String player2Id;
    public int round;
    public String phase;
    public String startingPlayerId;
    public String activePlayerId;
    public String turnStage;
    public long deadlineMillis;
    public List<String> fields = new ArrayList<>();
    public List<String> columnSolutions = new ArrayList<>();
    public String finalSolution;
    public List<Long> openedFields = new ArrayList<>();
    public List<Long> solvedColumns = new ArrayList<>();
    public long player1Score;
    public long player2Score;
    public long player1SolvedRounds;
    public long player2SolvedRounds;

    public AssociationGameState() { }
    public boolean isFinished() { return "finished".equals(phase); }
    public boolean isRoundBreak() { return "roundBreak".equals(phase); }
    public boolean isOpened(int index) { return openedFields.contains((long) index); }
    public boolean isSolved(int column) { return solvedColumns.contains((long) column); }
    public boolean canOpenField() { return turnStage == null || "open".equals(turnStage); }
    public boolean canGuessColumn() { return "guess".equals(turnStage); }
    public boolean canGuessFinal() {
        return "guess".equals(turnStage) || "finalOnly".equals(turnStage);
    }
}
