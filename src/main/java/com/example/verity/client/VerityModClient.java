package com.example.verity.client;

import com.example.verity.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class VerityModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.WATCHER, WatcherRenderer::new);
        EntityRendererRegistry.register(ModEntities.TOTEM_GUARDIAN, WatcherRenderer::new);
        JumpscareManager.register();
    }
}
