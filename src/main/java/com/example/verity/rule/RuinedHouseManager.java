package com.example.verity.rule;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class RuinedHouseManager {

    private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();
    private static final Random RANDOM = new Random();

    private static final int CHECK_INTERVAL = 10;
    private static final double SPAWN_CHANCE = 1.0 / 3000.0;
    private static final int MIN_COOLDOWN = 12000;
    private static final int MAX_COOLDOWN = 24000;
    private static final double MIN_DISTANCE = 30.0;
    private static final double MAX_DISTANCE = 60.0;

    private RuinedHouseManager() {}

    public static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        int cooldown = COOLDOWN.getOrDefault(id, 0);
        if (cooldown > 0) {
            COOLDOWN.put(id, cooldown - CHECK_INTERVAL);
            return;
        }

        Level dimLevel = player.level();
        if (dimLevel.dimension() != Level.OVERWORLD) return;
        if (RANDOM.nextDouble() > SPAWN_CHANCE) return;

        ServerLevel level = (ServerLevel) dimLevel;
        BlockPos origin = findSpot(player, level);
        if (origin == null) return;

        buildRuinedHouse(level, origin);
        COOLDOWN.put(id, MIN_COOLDOWN + RANDOM.nextInt(Math.max(1, MAX_COOLDOWN - MIN_COOLDOWN)));
    }

    private static BlockPos findSpot(ServerPlayer player, ServerLevel level) {
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        double distance = MIN_DISTANCE + RANDOM.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
        int x = (int) (player.getX() + Math.cos(angle) * distance);
        int z = (int) (player.getZ() + Math.sin(angle) * distance);

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight() + 1) return null;

        return new BlockPos(x, y - 1, z);
    }

    private static void set(ServerLevel level, BlockPos origin, int dx, int dy, int dz, BlockState state) {
        level.setBlock(origin.offset(dx, dy, dz), state, 3);
    }

    private static void buildRuinedHouse(ServerLevel level, BlockPos origin) {
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                set(level, origin, x, 0, z, RANDOM.nextBoolean() ? cobble : planks);
            }
        }

        set(level, origin, 0, 1, 0, log);
        set(level, origin, 0, 2, 0, log);
        set(level, origin, 4, 1, 0, log);
        set(level, origin, 4, 2, 0, log);
        set(level, origin, 0, 1, 4, log);
        set(level, origin, 0, 2, 4, log);
        set(level, origin, 4, 1, 4, log);
        set(level, origin, 4, 2, 4, log);

        for (int x = 1; x < 4; x++) {
            for (int y = 1; y <= 2; y++) {
                if (x == 2) continue;
                boolean gap = (y == 2 && x == 3);
                set(level, origin, x, y, 0, gap ? air : planks);
            }
        }

        for (int x = 1; x < 4; x++) {
            for (int y = 1; y <= 2; y++) {
                boolean gap = (y == 2 && (x == 1 || x == 2));
                set(level, origin, x, y, 4, gap ? air : planks);
            }
        }

        for (int z = 1; z < 4; z++) {
            set(level, origin, 0, 1, z, planks);
            set(level, origin, 0, 2, z, z == 2 ? air : planks);
        }

        for (int z = 1; z < 4; z++) {
            set(level, origin, 4, 1, z, z == 3 ? cobble : planks);
            set(level, origin, 4, 2, z, planks);
        }

        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 2; z++) {
                if ((x + z) % 3 == 0) continue;
                set(level, origin, x, 3, z, planks);
            }
        }

        set(level, origin, 1, 1, 3, cobble);
        set(level, origin, 3, 1, 1, Blocks.COBWEB.defaultBlockState());

        BlockPos chestPos = origin.offset(3, 1, 3);
        set(level, origin, 3, 1, 3, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(Items.BREAD, 2 + RANDOM.nextInt(3)));
            chest.setItem(1, new ItemStack(Items.WHEAT, 3 + RANDOM.nextInt(4)));
            chest.setItem(2, new ItemStack(Items.WHEAT_SEEDS, 2 + RANDOM.nextInt(3)));
        }

        placeSign(level, origin.offset(1, 1, 1), "Hâlâ orada", "mısın?", "");
        placeSign(level, origin.offset(3, 1, 1), "Hayır,", "gittim...", "");
    }

    private static void placeSign(ServerLevel level, BlockPos pos, String line1, String line2, String line3) {
        level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            SignText text = sign.getFrontText()
                    .setMessage(0, Component.literal(line1))
                    .setMessage(1, Component.literal(line2))
                    .setMessage(2, Component.literal(line3));
            sign.setText(text, true);
            sign.setChanged();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
        }
    }
          }
