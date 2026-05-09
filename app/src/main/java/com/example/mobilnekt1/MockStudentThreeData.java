package com.example.mobilnekt1;

public final class MockStudentThreeData {
    public static final Association ASSOCIATION = new Association(
            new String[][]{
                    {"Kos", "Tabla", "Lopta", "Parket"},
                    {"Servis", "Mreza", "Reket", "Set"},
                    {"Gol", "Kopacke", "Penal", "Stadion"},
                    {"Bazen", "Kapica", "Staza", "Plivanje"}
            },
            new String[]{"Kosarka", "Tenis", "Fudbal", "Plivanje"},
            "Sport"
    );

    public static final String[] SKOCKO_SYMBOLS = {
            "Skocko", "Kvadrat", "Krug", "Srce", "Trougao", "Zvezda"
    };

    public static final int[] SKOCKO_COMBINATION = {0, 2, 5, 3};

    public static final NotificationItem[] NOTIFICATIONS = {
            new NotificationItem("Cet", "Nova poruka u regionalnom cetu", "09:15", false),
            new NotificationItem("Rangiranje", "Osvojili ste 3. mesto na nedeljnoj rang listi", "Juce", false),
            new NotificationItem("Nagrada", "Dobili ste 2 tokena za dnevne misije", "Petak", true),
            new NotificationItem("Ostalo", "Presli ste u novu ligu", "Cetvrtak", true)
    };

    private MockStudentThreeData() {
    }

    public static class Association {
        public final String[][] fields;
        public final String[] columnSolutions;
        public final String finalSolution;

        public Association(String[][] fields, String[] columnSolutions, String finalSolution) {
            this.fields = fields;
            this.columnSolutions = columnSolutions;
            this.finalSolution = finalSolution;
        }
    }

    public static class NotificationItem {
        public final String channel;
        public final String message;
        public final String time;
        public boolean read;

        public NotificationItem(String channel, String message, String time, boolean read) {
            this.channel = channel;
            this.message = message;
            this.time = time;
            this.read = read;
        }
    }
}
