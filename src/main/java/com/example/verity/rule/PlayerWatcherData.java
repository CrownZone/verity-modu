package com.example.verity.rule;

import java.util.UUID;

/** Per-player state tracked by the RuleManager. */
public class PlayerWatcherData {
    public WatcherState state = WatcherState.DORMANT;
    public UUID watcherId = null;
    public int lookTimer = 0;
    public int awayTimer = 0;
    public int cooldown = 0;

    // Sleep-watcher feature: independent of the main dormant/watching cycle.
    public boolean wasSleeping = false;
    public UUID sleepWatcherId = null;

    // Morning stalker feature: independent of the other two.
    public UUID morningWatcherId = null;
    public int morningCooldown = 0;
}
