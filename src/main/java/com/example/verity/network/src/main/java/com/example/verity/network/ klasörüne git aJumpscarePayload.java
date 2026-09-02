package com.example.verity.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent from server to client when a player gets caught by the Watcher.
 * `variant` picks which jumpscare image/sound to show: 0 = original, 1 = new one.
 */
public record JumpscarePayload(int variant) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<JumpscarePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("verity_horror", "jumpscare"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JumpscarePayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeVarInt(payload.variant()),
                    buf -> new JumpscarePayload(buf.readVarInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
