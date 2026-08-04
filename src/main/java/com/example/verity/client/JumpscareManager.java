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

import java.util.Random;

/**
 * At random intervals (2, 3, 5, 6, or 7 minutes apart), briefly covers the
 * whole screen with a jumpscare image and plays an unsettling sound. Purely
 * a client-side effect - it doesn't affect gameplay or the Watcher rule
 * mechanic.
 */
public final class JumpscareManager {

    private static final ResourceLocation JUMPSCARE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("verity_horror", "textures/gui/jumpscare.png");

    private static final int[] INTERVAL_OPTIONS_MINUTES = {2, 3, 5, 6, 7};
    private static final int DISPLAY_TICKS = 30; // ~1.5 seconds on screen

    private static final Random RANDOM = new Random();

    private static int tickCounter = 0;
    private static int nextIntervalTicks = randomIntervalTicks();
    private static int displayTimer = 0;

    private JumpscareManager() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(JumpscareManager::onTick);
        HudRenderCallback.EVENT.register(JumpscareManager::onHudRender);
    }

    private static int randomIntervalTicks() {
        int minutes = INTERVAL_OPTIONS_MINUTES[RANDOM.nextInt(INTERVAL_OPTIONS_MINUTES.length)];
        return minutes * 1200; // 20 ticks/sec * 60
    }

    private static void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        if (displayTimer > 0) {
            displayTimer--;
            return;
        }

        tickCounter++;
        if (tickCounter >= nextIntervalTicks) {
            tickCounter = 0;
            nextIntervalTicks = randomIntervalTicks();
            trigger(client);
        }
    }

    private static void trigger(Minecraft client) {
        displayTimer = DISPLAY_TICKS;

        SoundEvent scarySound = BuiltInRegistries.SOUND_EVENT.get(
                ResourceLocation.withDefaultNamespace("entity.ghast.scream"));

        if (scarySound != null) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(scarySound, 1.0F));
        }
    }

    private static void onHudRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (displayTimer <= 0) return;

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        guiGraphics.blit(JUMPSCARE_TEXTURE, 0, 0, 0, 0, width, height, width, height);
    }
                }
