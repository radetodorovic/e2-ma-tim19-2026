package com.example.mobilnekt1.games.associations.domain;

public final class AssociationScoringEngine {
    private AssociationScoringEngine() { }

    public static int columnPoints(int openedFields) {
        return 2 + Math.max(0, 4 - openedFields);
    }

    public static int finalPoints(int[] openedFields, boolean[] solvedColumns) {
        int points = 7;
        for (int i = 0; i < 4; i++) {
            if (!solvedColumns[i]) {
                points += columnPoints(openedFields[i]);
            }
        }
        return points;
    }
}
