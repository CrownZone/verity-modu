package com.example.verity.rule;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * "Cesaret Totemi": a gold block surrounded by 4 netherrack blocks (cardinal
 * directions) with one more netherrack placed on top of the gold block.
 * Lighting that top netherrack with flint and steel shows a taunting message.
 * Purely a flavour/easter-egg mechanic -- doesn't affect gameplay otherwise.
 */
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

            return InteractionResult.PASS;
        });
    }
              }
