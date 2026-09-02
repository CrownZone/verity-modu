package com.example.verity.client;

import com.example.verity.entity.ModEntities;
import com.example.verity.network.JumpscarePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class VerityModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.WATCHER, WatcherRenderer::new);
        EntityRendererRegistry.register(ModEntities.TOTEM_GUARDIAN, WatcherRenderer::new);
        EntityRendererRegistry.register(ModEntities.FINAL_VERITY, WatcherRenderer::new);
        JumpscareManager.register();

        ClientPlayNetworking.registerGlobalReceiver(JumpscarePayload.TYPE, (payload, context) ->
                context.client().execute(() -> JumpscareManager.triggerCatch(context.client(), payload.variant()))
        );
    }
}
