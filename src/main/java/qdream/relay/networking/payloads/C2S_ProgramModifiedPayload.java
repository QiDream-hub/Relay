package qdream.relay.networking.payloads;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import qdream.relay.Relay;

/**
 * C2S: 客户端程序修改后发送到服务端
 */
public record C2S_ProgramModifiedPayload(CompoundTag programNbt) implements CustomPacketPayload {

    public static final Type<C2S_ProgramModifiedPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(Relay.MOD_ID, "c2s_program_modified")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_ProgramModifiedPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.COMPOUND_TAG,
        C2S_ProgramModifiedPayload::programNbt,
        C2S_ProgramModifiedPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
