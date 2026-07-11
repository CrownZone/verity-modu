package com.example.verity.rule;

public enum WatcherState {
    /** Nothing is happening. She may appear at any moment. */
    DORMANT,
    /** She has appeared. The rule is active: keep looking at her. */
    WATCHING
}
