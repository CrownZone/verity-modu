package com.example.verity.rule;

import java.util.UUID;

/** Per-player state tracked by the RuleManager. */
public class PlayerWatcherData {
    public WatcherState state = WatcherState.DORMANT;
    public UUID watcherId = null;
    public int lookTimer = 0;
    public int awayTimer = 0;
    public int cooldown = 0;

    public boolean wasSleeping = false;
    public UUID sleepWatcherId = null;

    public UUID morningWatcherId = null;
    public int morningCooldown = 0;

    public int ambientCooldown = 200;

    // Peripheral crawler: seen only out of the corner of your eye.
    public UUID peripheralWatcherId = null;
    public int peripheralCooldown = 200;
    public int peripheralTimer = 0;
}
