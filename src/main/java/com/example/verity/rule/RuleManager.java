package com.example.verity.rule;

import com.example.verity.entity.FinalVerityEntity;
import com.example.verity.entity.ModEntities;
import com.example.verity.entity.WatcherEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
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

public final class RuleManager {

    private static final Map<UUID, PlayerWatcherData> DATA = new HashMap<>();
    private static final Random RANDOM = new Random();

    private static final int CHECK_INTERVAL = 10;
    private static final int MIN_COOLDOWN = 6000;
    private static final int MAX_COOLDOWN = 12000;
    private static final double SPAWN_CHANCE = 1.0 / 240.0;
    private static final double INITIAL_DISTANCE = 26.0;
    private static final double APPROACH_STEP = 7.0;
    private static final double CATCH_DISTANCE = 3.0;
    private static final double LOOK_ANGLE_DEGREES = 20.0;
    private static final double MAX_SIGHT_DISTANCE = 48.0;
    private static final int LOOK_AWAY_GRACE = 20;
    private static final int SURVIVE_TICKS_REQUIRED = 100;
    private static final double SLEEP_WATCHER_CHANCE = 0.5;
    private static final double SLEEP_WATCHER_DISTANCE = 2.0;

    private static final int MORNING_START = 0;
    private static final int MORNING_END = 3000;
    private static final double MORNING_SPAWN_CHANCE = 1.0 / 200.0;
    private static final int TREE_SEARCH_RADIUS = 10;
    private static final int TREE_SEARCH_ATTEMPTS = 40;
    private static final double MORNING_TELEPORT_DISTANCE = 1.5;
    private static final float MORNING_DAMAGE = 1.0F;
    private static final int MORNING_BLINDNESS_TICKS = 200;

    private static final int AMBIENT_MIN_COOLDOWN = 400;
    private static final int AMBIENT_MAX_COOLDOWN = 2000;
    private static final double AMBIENT_MIN_DISTANCE = 4.0;
    private static final double AMBIENT_MAX_DISTANCE = 10.0;
    private static final String[] AMBIENT_SOUNDS = {
            "block.wood.break",
            "block.wood.hit",
            "block.stone.break",
            "block.stone.hit"
    };
    private static final String[] WHISPER_SOUNDS = {
            "entity.enderman.ambient",
            "entity.vex.ambient"
    };
    private static final double WHISPER_CHANCE = 0.25;

    private static final double PERIPHERAL_SPAWN_CHANCE = 1.0 / 300.0;
    private static final double PERIPHERAL_MIN_ANGLE = 35.0;
    private static final double PERIPHERAL_MAX_ANGLE = 75.0;
    private static final double PERIPHERAL_DISTANCE = 10.0;
    private static final int PERIPHERAL_LIFETIME = 100;
    private static final int PERIPHERAL_MIN_COOLDOWN = 2400;
    private static final int PERIPHERAL_MAX_COOLDOWN = 6000;

    private static final int FINAL_SURVIVE_THRESHOLD = 5;
    private static final int FINAL_MESSAGE_DELAY = 60;
    private static final int FINAL_SPAWN_DELAY = 60;

    private RuleManager() {}

    private static SoundEvent sound(String path) {
        return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.withDefaultNamespace(path));
    }

    private static void grantAdvancement(ServerPlayer player, String id) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source,
                "advancement grant " + player.getGameProfile().getName() + " only verity_horror:" + id);
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;

            PlayerWatcherData data = DATA.computeIfAbsent(player.getUUID(), id -> new PlayerWatcherData());

            handleSleep(player, data);
            handleMorningStalker(player, data);
            handleAmbientSounds(player, data);
            handlePeripheralCrawler(player, data);
            handleFinalSequence(player, data);
            RuinedHouseManager.tick(player);

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

    // --- Final boss sequence ---------------------------------------------------------

    private static void handleFinalSequence(ServerPlayer player, PlayerWatcherData data) {
        if (data.finalStage == 0) return;

        data.finalTimer += CHECK_INTERVAL;

        switch (data.finalStage) {
            case 1 -> {
                if (data.finalTimer == CHECK_INTERVAL) {
                    player.displayClientMessage(Component.literal("§4§lBeni yenemezsin. Gel yanıma."), false);
                }
                if (data.finalTimer >= FINAL_MESSAGE_DELAY) {
                    data.finalStage = 2;
                    data.finalTimer = 0;
                }
            }
            case 2 -> {
                if (data.finalTimer == CHECK_INTERVAL) {
                    player.displayClientMessage(Component.literal("§7Emin misin?"), false);
                }
                if (data.finalTimer >= FINAL_SPAWN_DELAY) {
                    spawnFinalVerity(player, data);
                    data.finalStage = 3;
                    data.finalTimer = 0;
                }
            }
            case 3 -> {
                ServerLevel level = (ServerLevel) player.level();
                Entity entity = data.finalBossId == null ? null : level.getEntity(data.finalBossId);
                if (!(entity instanceof FinalVerityEntity finalBoss) || !finalBoss.isAlive()) {
                    data.finalStage = 0;
                    data.finalBossId = null;
                    data.finalBossDefeated = true;
                    grantAdvancement(player, "defeated_verity");
                    player.displayClientMessage(Component.literal("§6§lVerity yenildi."), true);
                }
            }
        }
    }

    private static void spawnFinalVerity(ServerPlayer player, PlayerWatcherData data) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 spot = eye.add(look.scale(4.0));

        FinalVerityEntity boss = new FinalVerityEntity(ModEntities.FINAL_VERITY, level);
        boss.moveTo(spot.x, spot.y, spot.z, 0, 0);
        boss.setCustomName(Component.literal("§4§lVerity"));
        boss.setCustomNameVisible(true);
        boss.setTarget(player);
        level.addFreshEntity(boss);

        data.finalBossId = boss.getUUID();

        SoundEvent roar = sound("entity.ender_dragon.growl");
        if (roar != null) {
            level.playSound(null, boss.blockPosition(), roar, SoundSource.HOSTILE, 1.5F, 0.7F);
        }
    }

    // --- Ambient sounds & whispers ---------------------------------------------------------

    private static void handleAmbientSounds(ServerPlayer player, PlayerWatcherData data) {
        data.ambientCooldown -= CHECK_INTERVAL;
        if (data.ambientCooldown > 0) return;

        ServerLevel level = (ServerLevel) player.level();

        if (RANDOM.nextDouble() < WHISPER_CHANCE) {
            String path = WHISPER_SOUNDS[RANDOM.nextInt(WHISPER_SOUNDS.length)];
            SoundEvent event = sound(path);
            if (event != null) {
                float pitch = 0.5F + RANDOM.nextFloat() * 0.3F;
                level.playSound(null, player.blockPosition(), event, SoundSource.AMBIENT, 0.4F, pitch);
            }
            if (RANDOM.nextDouble() < 0.4) {
                player.displayClientMessage(Component.literal("§8...bir fısıltı duydun."), true);
            }
        } else {
            String path = AMBIENT_SOUNDS[RANDOM.nextInt(AMBIENT_SOUNDS.length)];
            SoundEvent event = sound(path);
            if (event != null) {
                double angle = RANDOM.nextDouble() * Math.PI * 2;
                double distance = AMBIENT_MIN_DISTANCE + RANDOM.nextDouble() * (AMBIENT_MAX_DISTANCE - AMBIENT_MIN_DISTANCE);
                double x = player.getX() + Math.cos(angle) * distance;
                double z = player.getZ() + Math.sin(angle) * distance;
                BlockPos pos = new BlockPos((int) x, player.blockPosition().getY(), (int) z);

                float pitch = 0.7F + RANDOM.nextFloat() * 0.5F;
                level.playSound(null, pos, event, SoundSource.AMBIENT, 0.8F, pitch);
            }
        }

        data.ambientCooldown = AMBIENT_MIN_COOLDOWN + RANDOM.nextInt(Math.max(1, AMBIENT_MAX_COOLDOWN - AMBIENT_MIN_COOLDOWN));
    }

    // --- Peripheral crawler ---------------------------------------------------------

    private static void handlePeripheralCrawler(ServerPlayer player, PlayerWatcherData data) {
        if (data.peripheralCooldown > 0) {
            data.peripheralCooldown -= CHECK_INTERVAL;
            return;
        }

        ServerLevel level = (ServerLevel) player.level();

        if (data.peripheralWatcherId != null) {
            Entity entity = level.getEntity(data.peripheralWatcherId);
            if (!(entity instanceof WatcherEntity watcher) || !watcher.isAlive()) {
                data.peripheralWatcherId = null;
                data.peripheralCooldown = PERIPHERAL_MIN_COOLDOWN + RANDOM.nextInt(Math.max(1, PERIPHERAL_MAX_COOLDOWN - PERIPHERAL_MIN_COOLDOWN));
                return;
            }

            data.peripheralTimer += CHECK_INTERVAL;

            if (isLookingAt(player, watcher)) {
                SoundEvent vanish = sound("entity.enderman.teleport");
                if (vanish != null) {
                    level.playSound(null, watcher.blockPosition(), vanish, SoundSource.AMBIENT, 0.6F, 1.3F);
                }
                player.displayClientMessage(Component.literal("§8Bir şey vardı... ama şimdi yok."), true);
                watcher.discard();
                data.peripheralWatcherId = null;
                data.peripheralCooldown = PERIPHERAL_MIN_COOLDOWN + RANDOM.nextInt(Math.max(1, PERIPHERAL_MAX_COOLDOWN - PERIPHERAL_MIN_COOLDOWN));
            } else if (data.peripheralTimer >= PERIPHERAL_LIFETIME) {
                watcher.discard();
                data.peripheralWatcherId = null;
                data.peripheralCooldown = PERIPHERAL_MIN_COOLDOWN + RANDOM.nextInt(Math.max(1, PERIPHERAL_MAX_COOLDOWN - PERIPHERAL_MIN_COOLDOWN));
            }
            return;
        }

        if (level.dimension() != Level.OVERWORLD) return;
        if (level.isDay()) return;
        if (RANDOM.nextDouble() > PERIPHERAL_SPAWN_CHANCE) return;

        Vec3 spawnPos = findPeripheralSpot(player);
        if (spawnPos == null) return;

        WatcherEntity watcher = new WatcherEntity(ModEntities.WATCHER, level);
        watcher.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0, 0);
        faceTowards(watcher, player);
        level.addFreshEntity(watcher);

        data.peripheralWatcherId = watcher.getUUID();
        data.peripheralTimer = 0;
    }

    private static Vec3 findPeripheralSpot(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        double offsetDeg = PERIPHERAL_MIN_ANGLE + RANDOM.nextDouble() * (PERIPHERAL_MAX_ANGLE - PERIPHERAL_MIN_ANGLE);
        double side = RANDOM.nextBoolean() ? 1.0 : -1.0;
        double angle = Math.toRadians(player.getYRot() + side * offsetDeg);

        double x = player.getX() - Math.sin(angle) * PERIPHERAL_DISTANCE;
        double z = player.getZ() + Math.cos(angle) * PERIPHERAL_DISTANCE;

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);
        if (y <= level.getMinBuildHeight() + 1) return null;

        return new Vec3(x, y, z);
    }

    // --- Sleep watcher ---------------------------------------------------------

    private static void handleSleep(ServerPlayer player, PlayerWatcherData data) {
        boolean sleepingNow = player.isSleeping();

        if (sleepingNow && !data.wasSleeping) {
            data.wasSleeping = true;
            if (RANDOM.nextDouble() < SLEEP_WATCHER_CHANCE) {
                spawnSleepWatcher(player, data);
            }
        } else if (!sleepingNow && data.wasSleeping) {
            data.wasSleeping = false;
            if (data.sleepWatcherId != null) {
                ServerLevel level = (ServerLevel) player.level();
                Entity entity = level.getEntity(data.sleepWatcherId);
                if (entity != null) entity.discard();
                data.sleepWatcherId = null;
            }
        }
    }

    private static void spawnSleepWatcher(ServerPlayer player, PlayerWatcherData data) {
        ServerLevel level = (ServerLevel) player.level();

        Vec3 bedPos = player.getSleepingPos()
                .map(Vec3::atCenterOf)
                .orElse(player.position());

        double yaw = Math.toRadians(player.getYRot());
        double dx = -Math.sin(yaw) * SLEEP_WATCHER_DISTANCE;
        double dz = Math.cos(yaw) * SLEEP_WATCHER_DISTANCE;

        WatcherEntity watcher = new WatcherEntity(ModEntities.WATCHER, level);
        watcher.moveTo(bedPos.x + dx, bedPos.y, bedPos.z + dz, 0, 0);
        faceTowards(watcher, player);

        level.addFreshEntity(watcher);
        data.sleepWatcherId = watcher.getUUID();

        SoundEvent laugh = sound("entity.witch.celebrate");
        if (laugh != null) {
            level.playSound(null, watcher.blockPosition(), laugh, SoundSource.HOSTILE, 1.0F, 0.8F);
        }
    }

    // --- Morning stalker ---------------------------------------------------------

    private static void handleMorningStalker(ServerPlayer player, PlayerWatcherData data) {
        if (data.morningCooldown > 0) {
            data.morningCooldown -= CHECK_INTERVAL;
            return;
        }

        ServerLevel level = (ServerLevel) player.level();

        if (data.morningWatcherId != null) {
            Entity entity = level.getEntity(data.morningWatcherId);
            if (!(entity instanceof WatcherEntity watcher) || !watcher.isAlive()) {
                data.morningWatcherId = null;
                return;
            }

            if (isLookingAt(player, watcher)) {
                triggerMorningCatch(player, watcher, data);
            }
            return;
        }

        if (level.dimension() != Level.OVERWORLD) return;
        long time = level.getDayTime() % 24000L;
        if (time < MORNING_START || time > MORNING_END) return;
        if (RANDOM.nextDouble() > MORNING_SPAWN_CHANCE) return;

        BlockPos treePos = findNearbyTree(player);
        if (treePos == null) return;

        Vec3 spawnPos = spotBehindTree(player, treePos);
        if (spawnPos == null) return;

        WatcherEntity watcher = new WatcherEntity(ModEntities.WATCHER, level);
        watcher.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0, 0);
        faceTowards(watcher, player);
        level.addFreshEntity(watcher);

        data.morningWatcherId = watcher.getUUID();
    }

    private static BlockPos findNearbyTree(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = player.blockPosition();

        for (int i = 0; i < TREE_SEARCH_ATTEMPTS; i++) {
            int dx = RANDOM.nextInt(TREE_SEARCH_RADIUS * 2 + 1) - TREE_SEARCH_RADIUS;
            int dy = RANDOM.nextInt(9) - 4;
            int dz = RANDOM.nextInt(TREE_SEARCH_RADIUS * 2 + 1) - TREE_SEARCH_RADIUS;
            BlockPos pos = origin.offset(dx, dy, dz);

            if (level.getBlockState(pos).is(BlockTags.LOGS)) {
                return pos;
            }
        }
        return null;
    }

    private static Vec3 spotBehindTree(ServerPlayer player, BlockPos treePos) {
        double dx = treePos.getX() + 0.5 - player.getX();
        double dz = treePos.getZ() + 0.5 - player.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001) return null;

        double nx = dx / length;
        double nz = dz / length;

        double x = treePos.getX() + 0.5 + nx;
        double z = treePos.getZ() + 0.5 + nz;

        ServerLevel level = (ServerLevel) player.level();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);
        if (y <= level.getMinBuildHeight() + 1) {
            y = treePos.getY();
        }

        return new Vec3(x, y, z);
    }

    private static void triggerMorningCatch(ServerPlayer player, WatcherEntity watcher, PlayerWatcherData data) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 frontPos = eye.add(look.scale(MORNING_TELEPORT_DISTANCE));

        watcher.teleportTo(frontPos.x, frontPos.y, frontPos.z);
        faceTowards(watcher, player);

        ServerLevel level = (ServerLevel) player.level();

        SoundEvent scream = sound("entity.ghast.scream");
        if (scream != null) {
            level.playSound(null, player.blockPosition(), scream, SoundSource.HOSTILE, 1.0F, 1.4F);
        }

        player.hurt(level.damageSources().magic(), MORNING_DAMAGE);

        try {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.BLINDNESS, MORNING_BLINDNESS_TICKS, 0));
        } catch (Exception ignored) {
        }

        player.displayClientMessage(Component.literal("§4§lSeni gördü."), true);

        watcher.discard();
        data.morningWatcherId = null;
        data.morningCooldown = MIN_COOLDOWN + RANDOM.nextInt(Math.max(1, MAX_COOLDOWN - MIN_COOLDOWN));

        grantAdvancement(player, "caught");
    }

    // --- Night watcher: spawning ---------------------------------------------------------

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

        player.displayClientMessage(Component.literal("§7Bir şey seni izliyor..."), true);
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

    // --- Night watcher: active encounter ---------------------------------------------------------

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
                player.displayClientMessage(Component.literal("§8Kayboldu..."), true);
                resetToDormant(data);
                data.cooldown = (MIN_COOLDOWN + MAX_COOLDOWN) / 4;
                grantAdvancement(player, "survived");

                data.survivedCount++;
                if (data.survivedCount >= FINAL_SURVIVE_THRESHOLD && !data.finalBossDefeated && data.finalStage == 0) {
                    data.finalStage = 1;
                    data.finalTimer = 0;
                }
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

        SoundEvent heartbeat = sound("entity.warden.heartbeat");
        if (heartbeat != null) {
            player.level().playSound(null, watcher.blockPosition(), heartbeat, SoundSource.HOSTILE, 1.5F, 0.6F);
        }
        SoundEvent cave = sound("ambient.cave");
        if (cave != null) {
            player.level().playSound(null, player.blockPosition(), cave, SoundSource.AMBIENT, 1.0F, 0.5F);
        }
        player.displayClientMessage(Component.literal("§4Ona bakmayı bıraktın."), true);
    }

    private static void jumpscare(ServerPlayer player, WatcherEntity watcher, PlayerWatcherData data) {
        ServerLevel level = (ServerLevel) player.level();

        SoundEvent explode = sound("entity.generic.explode");
        if (explode != null) {
            level.playSound(null, player.blockPosition(), explode, SoundSource.HOSTILE, 1.2F, 0.5F);
        }
        SoundEvent heartbeat = sound("entity.warden.heartbeat");
        if (heartbeat != null) {
            level.playSound(null, player.blockPosition(), heartbeat, SoundSource.HOSTILE, 2.0F, 0.3F);
        }

        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.BLINDNESS, 100, 0));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.CONFUSION, 140, 0));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.WEAKNESS, 200, 1));

        player.setHealth(1.0F);

        watcher.discard();
        player.displayClientMessage(Component.literal("§c§lYAKALANDIN."), true);

        resetToDormant(data);
        data.cooldown = MIN_COOLDOWN + RANDOM.nextInt(Math.max(1, MAX_COOLDOWN - MIN_COOLDOWN));

        grantAdvancement(player, "caught");
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
