package com.example.mobilnekt1.games.quiz.data;

import com.example.mobilnekt1.games.quiz.domain.QuizGameState;

public interface QuizGameListener {
    void onChanged(QuizGameState state);

    void onError(String message);
}
