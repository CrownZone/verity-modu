package com.example.verity.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Every few minutes, briefly covers the whole screen with a jumpscare image
 * and plays an unsettling sound. Purely a client-side effect (cosmetic jump
 * scare) - it doesn't affect gameplay or the Watcher rule mechanic.
 */
public final class JumpscareManager {

    private static final ResourceLocation JUMPSCARE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("verity_horror", "textures/gui/jumpscare.png");

    private static final int INTERVAL_TICKS = 6000;  // 5 minutes (20 ticks/sec)
    private static final int DISPLAY_TICKS = 30;      // ~1.5 seconds on screen

    private static int tickCounter = 0;
    private static int displayTimer = 0;

    private JumpscareManager() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(JumpscareManager::onTick);
        HudRenderCallback.EVENT.register(JumpscareManager::onHudRender);
    }

    private static void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        if (displayTimer > 0) {
            displayTimer--;
            return;
        }

        tickCounter++;
        if (tickCounter >= INTERVAL_TICKS) {
            tickCounter = 0;
            trigger(client);
        }
    }

    private static void trigger(Minecraft client) {
        displayTimer = DISPLAY_TICKS;

        SoundEvent scarySound = BuiltInRegistries.SOUND_EVENT.get(
                ResourceLocation.withDefaultNamespace("entity.warden.roar"));

        if (scarySound != null) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(scarySound, 1.0F));
        }
    }

    private static void onHudRender(GuiGraphics guiGraphics, float tickDelta) {
        if (displayTimer <= 0) return;

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        guiGraphics.blit(JUMPSCARE_TEXTURE, 0, 0, 0, 0, width, height, width, height);
    }
}
