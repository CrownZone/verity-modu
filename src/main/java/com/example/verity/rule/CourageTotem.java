package com.example.verity.rule;

import com.example.verity.entity.ModEntities;
import com.example.verity.entity.TotemGuardianEntity;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public final class CourageTotem {

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
        TotemGuardianEntity guardian = new TotemGuardianEntity(ModEntities.TOTEM_GUARDIAN, level);
        guardian.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);
        guardian.setCustomName(Component.literal("§4Totem Bekçisi"));
        guardian.setCustomNameVisible(true);
        guardian.setTarget(player);
        level.addFreshEntity(guardian);
    }
}
