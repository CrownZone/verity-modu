package com.example.verity;

import com.example.verity.entity.ModEntities;
import com.example.verity.item.ModItems;
import com.example.verity.rule.CourageTotem;
import com.example.verity.rule.RuleManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class VerityMod implements ModInitializer {

    public static final String MOD_ID = "verity_horror";

    @Override
    public void onInitialize() {
        ModEntities.register();
        ModItems.register();
        ServerTickEvents.END_SERVER_TICK.register(RuleManager::tick);
        CourageTotem.register();
    }
}
