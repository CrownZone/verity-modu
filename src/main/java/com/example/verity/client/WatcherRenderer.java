package com.example.verity.client;

import com.example.verity.VerityMod;
import com.example.verity.entity.WatcherEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Watcher using the vanilla zombie model/animations, but with our
 * own texture instead of the vanilla zombie skin. This keeps normal zombies
 * unaffected -- only the Watcher gets this look.
 */
public class WatcherRenderer extends ZombieRenderer {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VerityMod.MOD_ID, "textures/entity/watcher/watcher.png");

    public WatcherRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(net.minecraft.world.entity.monster.Zombie entity) {
        return TEXTURE;
    }
}
