package qdream.relay.networking.payloads;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;

/**
 * 客户端 → 服务端：请求操作列表
 */
public record C2S_RequestOperationsPayload() implements CustomPacketPayload {
    public static final Type<C2S_RequestOperationsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Relay.MOD_ID, "c2s_request_operations"));
    public static final StreamCodec<FriendlyByteBuf, C2S_RequestOperationsPayload> CODEC = StreamCodec.unit(
            new C2S_RequestOperationsPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
