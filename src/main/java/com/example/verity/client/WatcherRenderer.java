package com.example.verity.client;

import com.example.verity.VerityMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Renders the Watcher (and Totem Guardian / Final Verity, which reuse this
 * renderer) using a plain humanoid model with a normal, arms-down Steve-like
 * posture instead of the vanilla zombie's arms-forward shuffle pose.
 */
public class WatcherRenderer extends HumanoidMobRenderer<Zombie, HumanoidModel<Zombie>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(VerityMod.MOD_ID, "textures/entity/watcher/watcher.png");

    public WatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie entity) {
        return TEXTURE;
    }
}
