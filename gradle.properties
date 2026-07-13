package com.example.verity.rule;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * "Cesaret Totemi": a gold block surrounded by 4 netherrack blocks (cardinal
 * directions) with one more netherrack placed on top of the gold block.
 * Lighting that top netherrack with flint and steel shows a taunting message
 * and summons a hostile guardian (50 HP, 3.5 attack damage) to punish the
 * player for their arrogance.
 */
public final class CourageTotem {

    private static final double GUARDIAN_HEALTH = 50.0;
    private static final double GUARDIAN_ATTACK_DAMAGE = 3.5;

    private CourageTotem() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Items.FLINT_AND_STEEL)) return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            if (!world.getBlockState(pos).is(Blocks.NETHERRACK)) return InteractionResult.PASS;

            BlockPos goldPos = pos.below();
            if (!world.getBlockState(goldPos).is(Blocks.GOLD_BLOCK)) return InteractionResult.PASS;

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (!world.getBlockState(goldPos.relative(dir)).is(Blocks.NETHERRACK)) {
                    return InteractionResult.PASS;
                }
            }

            serverPlayer.displayClientMessage(
                    Component.literal("§4§lCesaret edemezsin... §7bunun bir anlamı yok."),
                    true
            );

            summonGuardian((ServerLevel) world, pos, serverPlayer);

            return InteractionResult.PASS;
        });
    }

    private static void summonGuardian(ServerLevel level, BlockPos pos, ServerPlayer player) {
        Zombie guardian = EntityType.ZOMBIE.create(level);
        if (guardian == null) return;

        guardian.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);

        if (guardian.getAttribute(Attributes.MAX_HEALTH) != null) {
            guardian.getAttribute(Attributes.MAX_HEALTH).setBaseValue(GUARDIAN_HEALTH);
        }
        guardian.setHealth((float) GUARDIAN_HEALTH);

        if (guardian.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            guardian.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(GUARDIAN_ATTACK_DAMAGE);
        }

        guardian.setCustomName(Component.literal("§4Totem Bekçisi"));
        guardian.setCustomNameVisible(true);
        guardian.setPersistenceRequired();
        guardian.setTarget(player);

        level.addFreshEntity(guardian);
    }
}
