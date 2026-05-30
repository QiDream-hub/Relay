package qdream.relay.networking.payloads;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;

/**
 * 客户端 → 服务端：更新程序
 */
public record C2S_UpdateProgramPayload(int slotId, byte[] programNbt) implements CustomPacketPayload {
    public static final Type<C2S_UpdateProgramPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Relay.MOD_ID, "c2s_update_program"));
    public static final StreamCodec<FriendlyByteBuf, C2S_UpdateProgramPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, C2S_UpdateProgramPayload::slotId,
            ByteBufCodecs.BYTE_ARRAY, C2S_UpdateProgramPayload::programNbt,
            C2S_UpdateProgramPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
