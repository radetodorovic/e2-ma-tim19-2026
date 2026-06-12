package com.example.mobilnekt1.games.stepbystep;

public final class StepPuzzleRepository {
    private static final StepPuzzle[] PUZZLES = {
            new StepPuzzle("Android", new String[]{
                    "Koristi se svakodnevno na velikom broju uredjaja.",
                    "Ima verzije sa imenima i brojevima.",
                    "Za njega se aplikacije prave u posebnom razvojnom okruzenju.",
                    "Cesto se koristi Java ili Kotlin.",
                    "Ima aktivnosti, fragmente i XML rasporede.",
                    "Pokrece se na mobilnim telefonima i tabletima.",
                    "Operativni sistem iz Google ekosistema."
            }),
            new StepPuzzle("Dunav", new String[]{
                    "Povezuje veliki broj gradova i kultura.",
                    "Njegov tok prolazi kroz vise evropskih drzava.",
                    "Vazan je za saobracaj i trgovinu.",
                    "Prolazi kroz Bec, Budimpestu i Beograd.",
                    "U Srbiju ulazi kod Batine.",
                    "Jedna je od najduzih evropskih reka.",
                    "Uliva se u Crno more."
            })
    };

    private StepPuzzleRepository() {
    }

    public static StepPuzzle forRound(int roundIndex) {
        return PUZZLES[roundIndex % PUZZLES.length];
    }
}
