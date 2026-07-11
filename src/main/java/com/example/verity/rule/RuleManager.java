package com.example.verity.rule;

import com.example.verity.entity.ModEntities;
import com.example.verity.entity.WatcherEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Drives the whole "Verity" mechanic: spawning the Watcher, checking whether
 * the player is keeping eye contact with her, punishing broken rules, and
 * resolving encounters (survive or get caught).
 *
 * Tune the constants below to make the mod easier/harder or more/less frequent.
 */
public final class RuleManager {

    private static final Map<UUID, PlayerWatcherData> DATA = new HashMap<>();
    private static final Random RANDOM = new Random();

    // --- Tuning ---------------------------------------------------------
    private static final int CHECK_INTERVAL = 10;          // run logic every 10 ticks (0.5s)
    private static final int MIN_COOLDOWN = 6000;           // 5 minutes minimum between encounters
    private static final int MAX_COOLDOWN = 12000;          // 10 minutes maximum
    private static final double SPAWN_CHANCE = 1.0 / 240.0; // rolled once every CHECK_INTERVAL while dormant & eligible
    private static final double INITIAL_DISTANCE = 26.0;    // blocks, how far away she first appears
    private static final double APPROACH_STEP = 7.0;        // blocks, how much closer she jumps per rule break
    private static final double CATCH_DISTANCE = 3.0;       // blocks, distance that triggers the jumpscare
    private static final double LOOK_ANGLE_DEGREES = 20.0;  // cone considered "looking at her"
    private static final double MAX_SIGHT_DISTANCE = 48.0;  // ignore eye-contact checks beyond this range
    private static final int LOOK_AWAY_GRACE = 20;          // ticks of grace before punishing a broken glance
    private static final int SURVIVE_TICKS_REQUIRED = 100;  // 5s of sustained eye contact needed to make her vanish

    private RuleManager() {}

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;

            PlayerWatcherData data = DATA.computeIfAbsent(player.getUUID(), id -> new PlayerWatcherData());

            if (data.cooldown > 0) {
                data.cooldown -= CHECK_INTERVAL;
                continue;
            }

            switch (data.state) {
                case DORMANT -> tryTrigger(player, data);
                case WATCHING -> updateWatching(player, data);
            }
        }
    }

    // --- Spawning ---------------------------------------------------------

    private static void tryTrigger(ServerPlayer player, PlayerWatcherData data) {
        Level level = player.level();
        if (level.dimension() != Level.OVERWORLD) return;
        if (level.isDay()) return;
        if (RANDOM.nextDouble() > SPAWN_CHANCE) return;

        Vec3 spawnPos = findSpawnSpot(player);
        if (spawnPos == null) return;

        ServerLevel serverLevel = (ServerLevel) level;
        WatcherEntity watcher = new WatcherEntity(ModEntities.WATCHER, serverLevel);
        watcher.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0, 0);
        faceTowards(watcher, player);
        serverLevel.addFreshEntity(watcher);

        data.state = WatcherState.WATCHING;
        data.watcherId = watcher.getUUID();
        data.lookTimer = 0;
        data.awayTimer = 0;

        player.displayClientMessage(Component.literal("§7Bir şey seni izliyor... §8(Ona bak ve gözünü ayırma)"), true);
    }

    private static Vec3 findSpawnSpot(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        double angle = Math.toRadians(player.getYRot() + 180 + (RANDOM.nextDouble() - 0.5) * 90);
        double x = player.getX() + Math.sin(angle) * -INITIAL_DISTANCE;
        double z = player.getZ() + Math.cos(angle) * INITIAL_DISTANCE;

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);
        if (y <= level.getMinBuildHeight() + 1) return null;

        return new Vec3(x, y, z);
    }

    // --- Active encounter ---------------------------------------------------------

    private static void updateWatching(ServerPlayer player, PlayerWatcherData data) {
        ServerLevel level = (ServerLevel) player.level();
        Entity entity = data.watcherId == null ? null : level.getEntity(data.watcherId);

        if (!(entity instanceof WatcherEntity watcher) || !watcher.isAlive()) {
            resetToDormant(data);
            return;
        }

        double distance = player.position().distanceTo(watcher.position());

        if (isLookingAt(player, watcher)) {
            data.lookTimer += CHECK_INTERVAL;
            data.awayTimer = 0;

            if (data.lookTimer >= SURVIVE_TICKS_REQUIRED) {
                watcher.discard();
                player.displayClientMessage(Component.literal("§8Kayboldu... Şimdilik güvendesin."), true);
                resetToDormant(data);
                data.cooldown = (MIN_COOLDOWN + MAX_COOLDOWN) / 4;
                return;
            }
        } else {
            data.lookTimer = 0;
            data.awayTimer += CHECK_INTERVAL;

            if (data.awayTimer >= LOOK_AWAY_GRACE) {
                approach(player, watcher);
                data.awayTimer = 0;
            }
        }

        if (distance <= CATCH_DISTANCE) {
            jumpscare(player, watcher, data);
        }
    }

    private static void approach(ServerPlayer player, WatcherEntity watcher) {
        Vec3 toPlayer = player.position().subtract(watcher.position());
        double currentDistance = toPlayer.length();
        double newDistance = Math.max(CATCH_DISTANCE - 0.5, currentDistance - APPROACH_STEP);

        if (currentDistance < 0.001) return;
        Vec3 direction = toPlayer.scale(1.0 / currentDistance);
        Vec3 newPos = player.position().subtract(direction.scale(newDistance));

        watcher.teleportTo(newPos.x, newPos.y, newPos.z);
        faceTowards(watcher, player);

        player.level().playSound(null, watcher.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.HOSTILE, 1.5F, 0.6F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMBIENT_CAVE,
                SoundSource.AMBIENT, 1.0F, 0.5F);
        player.displayClientMessage(Component.literal("§4Ona bakmayı bıraktın. Şimdi daha yakın."), true);
    }

    private static void jumpscare(ServerPlayer player, WatcherEntity watcher, PlayerWatcherData data) {
        ServerLevel level = (ServerLevel) player.level();

        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 1.2F, 0.5F);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.HOSTILE, 2.0F, 0.3F);

        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, 0));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));

        // NOTE: on some newer 1.21.x patches Entity#hurt was split into a
        // server-authoritative hurtServer(ServerLevel, DamageSource, float)
        // overload. If this line fails to compile after bumping minecraft_version,
        // switch it to: player.hurtServer(level, level.damageSources().magic(), 6.0F);
        player.hurt(level.damageSources().magic(), 6.0F);

        watcher.discard();
        player.displayClientMessage(Component.literal("§c§lYAKALANDIN."), true);

        resetToDormant(data);
        data.cooldown = MIN_COOLDOWN + RANDOM.nextInt(Math.max(1, MAX_COOLDOWN - MIN_COOLDOWN));
    }

    private static void resetToDormant(PlayerWatcherData data) {
        data.state = WatcherState.DORMANT;
        data.watcherId = null;
        data.lookTimer = 0;
        data.awayTimer = 0;
    }

    // --- Helpers ---------------------------------------------------------

    private static boolean isLookingAt(ServerPlayer player, WatcherEntity watcher) {
        Vec3 eye = player.getEyePosition();
        Vec3 target = watcher.getEyePosition();
        Vec3 toTarget = target.subtract(eye);

        double distance = toTarget.length();
        if (distance > MAX_SIGHT_DISTANCE || distance < 0.001) return false;

        Vec3 toTargetNormalized = toTarget.scale(1.0 / distance);
        Vec3 look = player.getViewVector(1.0F);

        double dot = Mth.clamp(look.dot(toTargetNormalized), -1.0, 1.0);
        double angleDegrees = Math.toDegrees(Math.acos(dot));
        if (angleDegrees > LOOK_ANGLE_DEGREES) return false;

        ClipContext ctx = new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        HitResult result = player.level().clip(ctx);
        return result.getType() == HitResult.Type.MISS;
    }

    private static void faceTowards(WatcherEntity watcher, ServerPlayer player) {
        double dx = player.getX() - watcher.getX();
        double dz = player.getZ() - watcher.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        watcher.setYRot(yaw);
        watcher.setYHeadRot(yaw);
        watcher.setYBodyRot(yaw);
    }
          }
