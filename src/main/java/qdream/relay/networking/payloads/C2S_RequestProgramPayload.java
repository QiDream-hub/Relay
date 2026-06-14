package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;

/**
 * 客户端 → 服务端：请求法术程序列表
 */
public record C2S_RequestProgramPayload() implements CustomPacketPayload {
    public static final Type<C2S_RequestProgramPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Relay.MOD_ID, "c2s_request_program"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_RequestProgramPayload> CODEC = StreamCodec.unit(new C2S_RequestProgramPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
