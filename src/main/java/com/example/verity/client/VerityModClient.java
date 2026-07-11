package com.example.verity.client;

import com.example.verity.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ZombieRenderer;

public class VerityModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Reuses the vanilla zombie model/renderer so the mod compiles and
        // renders reliably out of the box. See README.md for how to swap in
        // the included placeholder texture (assets/verity_horror/textures/
        // entity/watcher/watcher.png) with a fully custom renderer once you're
        // ready to give the Watcher her own distinct look.
        EntityRendererRegistry.register(ModEntities.WATCHER, ZombieRenderer::new);
    }
}
