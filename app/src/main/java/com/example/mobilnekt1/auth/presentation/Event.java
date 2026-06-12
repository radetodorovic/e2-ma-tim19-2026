package com.example.mobilnekt1.auth.presentation;

public final class Event<T> {
    private final T value;
    private boolean handled;

    public Event(T value) {
        this.value = value;
    }

    public T getIfNotHandled() {
        if (handled) {
            return null;
        }
        handled = true;
        return value;
    }
}
