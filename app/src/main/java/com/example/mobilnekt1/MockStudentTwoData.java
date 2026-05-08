package com.example.mobilnekt1;

public final class MockStudentTwoData {
    public static final String USERNAME = "milan19";
    public static final String EMAIL = "milan19@example.com";
    public static final String REGION = "Backa";
    public static final String LEAGUE = "Srebrna liga";
    public static final int TOKENS = 8;
    public static final int STARS = 245;

    public static final String[] AVATARS = {"M1", "M2", "M3", "M4"};

    public static final String[] STATISTICS = {
            "Prosecni bodovi po partiji: 126",
            "Ko zna zna: 18 pogodjenih / 7 promasenih",
            "Moj broj: tacan broj pronadjen u 64% rundi",
            "Korak po korak: korak 1 - 8%, korak 2 - 12%, korak 3 - 18%, korak 4 - 24%, korak 5 - 20%, korak 6 - 11%, korak 7 - 7%",
            "Asocijacije: 11 resenih / 5 neresenih",
            "Skocko: pokusaj 1 - 6%, pokusaj 2 - 14%, pokusaj 3 - 27%, pokusaj 4 - 21%, pokusaj 5 - 18%, pokusaj 6 - 14%",
            "Spojnice: 78% uspesno povezanih pojmova",
            "Ukupno odigranih partija: 34",
            "Pobede / porazi: 61% / 39%"
    };

    public static final QuizQuestion[] QUIZ_QUESTIONS = {
            new QuizQuestion("Koji je glavni grad Srbije?",
                    new String[]{"Novi Sad", "Beograd", "Nis", "Kragujevac"}, 1),
            new QuizQuestion("Koliko igraca ucestvuje u jednoj partiji?",
                    new String[]{"Jedan", "Dva", "Tri", "Cetiri"}, 1),
            new QuizQuestion("Koliko odgovora ima pitanje u igri Ko zna zna?",
                    new String[]{"Dva", "Tri", "Cetiri", "Pet"}, 2),
            new QuizQuestion("Koliko bodova nosi tacan odgovor?",
                    new String[]{"5", "10", "15", "20"}, 1),
            new QuizQuestion("Koliko sekundi traje jedno pitanje?",
                    new String[]{"3", "5", "10", "25"}, 1)
    };

    public static final String[] CONNECTION_LEFT = {
            "Nikola Tesla",
            "Mihajlo Pupin",
            "Ivo Andric",
            "Novak Djokovic",
            "Marina Abramovic"
    };

    public static final String[] CONNECTION_RIGHT = {
            "Tenis",
            "Performans",
            "Na Drini cuprija",
            "Naizmenicna struja",
            "Kalemovi"
    };

    public static final int[] CONNECTION_MATCHES = {3, 4, 2, 0, 1};

    public static final class QuizQuestion {
        public final String question;
        public final String[] answers;
        public final int correctIndex;

        public QuizQuestion(String question, String[] answers, int correctIndex) {
            this.question = question;
            this.answers = answers;
            this.correctIndex = correctIndex;
        }
    }

    private MockStudentTwoData() {
    }
}
