package com.example.verity.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Shows a brief full-screen jumpscare + sound when the server tells us the
 * player just got caught by the Watcher (see triggerCatch). Randomly picks
 * between the original image and a second one.
 *
 * The old random-interval ambient jumpscare (independent of gameplay) has
 * been removed - this now only fires on an actual catch.
 */
public final class JumpscareManager {

    private static final ResourceLocation JUMPSCARE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("verity_horror", "textures/gui/jumpscare.png");
    private static final ResourceLocation JUMPSCARE_TEXTURE_2 =
            ResourceLocation.fromNamespaceAndPath("verity_horror", "textures/gui/jumpscare2.png");

    private static final int DISPLAY_TICKS = 30; // ~1.5 seconds on screen

    private static int displayTimer = 0;
    private static ResourceLocation currentTexture = JUMPSCARE_TEXTURE;

    private JumpscareManager() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(JumpscareManager::onTick);
        HudRenderCallback.EVENT.register(JumpscareManager::onHudRender);
    }

    private static void onTick(Minecraft client) {
        if (displayTimer > 0) {
            displayTimer--;
        }
    }

    /**
     * Called when the server tells us the player just got caught by the Watcher.
     * variant 0 = original image + ghast scream, variant 1 = new image + wither spawn sound.
     */
    public static void triggerCatch(Minecraft client, int variant) {
        displayTimer = DISPLAY_TICKS;
        currentTexture = (variant == 1) ? JUMPSCARE_TEXTURE_2 : JUMPSCARE_TEXTURE;

        String soundPath = (variant == 1) ? "entity.wither.spawn" : "entity.ghast.scream";
        SoundEvent scarySound = BuiltInRegistries.SOUND_EVENT.get(
                ResourceLocation.withDefaultNamespace(soundPath));

        if (scarySound != null) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(scarySound, 1.0F));
        }
    }

    private static void onHudRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (displayTimer <= 0) return;

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        guiGraphics.blit(currentTexture, 0, 0, 0, 0, width, height, width, height);
    }
}
