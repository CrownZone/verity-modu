package com.example.verity;

import com.example.verity.entity.ModEntities;
import com.example.verity.rule.RuleManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Verity: The Watcher
 *
 * Verity modundan ilham alan bir psikolojik korku modu.
 * Geceleri, sessiz bir "Gözlemci" (Watcher) oyuncunun etrafında belirebilir.
 * Tek kural basit: ona bak ve gözünü ayırma. Ayırırsan yaklaşır. Yakalarsa...
 */
public class VerityMod implements ModInitializer {

    public static final String MOD_ID = "verity_horror";

    @Override
    public void onInitialize() {
        ModEntities.register();
        ServerTickEvents.END_SERVER_TICK.register(RuleManager::tick);
    }
}
