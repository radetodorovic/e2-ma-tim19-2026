package com.example.mobilnekt1.games.quiz.domain;

import java.util.ArrayList;
import java.util.List;

public final class QuizQuestion {
    public String text;
    public List<String> answers = new ArrayList<>();
    public int correctIndex;

    public QuizQuestion() {
    }

    public QuizQuestion(String text, List<String> answers, int correctIndex) {
        this.text = text;
        this.answers = answers;
        this.correctIndex = correctIndex;
    }
}
