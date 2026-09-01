package com.example.verity.ai;

public class WatcherDecision {
    public enum Action { FREEZE, STALK, ATTACK }

    public final String message;
    public final Action action;

    public WatcherDecision(String message, Action action) {
        this.message = message;
        this.action = action;
    }
}
