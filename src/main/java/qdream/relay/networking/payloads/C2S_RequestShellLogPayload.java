package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import qdream.relay.Relay;

/**
 * 客户端 → 服务端：请求 Shell 日志同步
 */
public record C2S_RequestShellLogPayload() implements CustomPacketPayload {

    public static final Type<C2S_RequestShellLogPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(Relay.MOD_ID, "request_shell_log")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_RequestShellLogPayload> CODEC =
        StreamCodec.unit(new C2S_RequestShellLogPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
