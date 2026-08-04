package com.example.verity.rule;

import java.util.UUID;

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

    public UUID peripheralWatcherId = null;
    public int peripheralCooldown = 200;
    public int peripheralTimer = 0;

    public int survivedCount = 0;
    public int finalStage = 0;
    public int finalTimer = 0;
    public UUID finalBossId = null;
    public boolean finalBossDefeated = false;

    // Forced confrontation: appears right in front of you, always repositions
    // to stay in front no matter where you look.
    public UUID confrontWatcherId = null;
    public int confrontCooldown = 400;
    public int confrontTimer = 0;
}
