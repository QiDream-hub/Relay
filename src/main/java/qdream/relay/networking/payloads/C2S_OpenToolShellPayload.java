package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;

/**
 * 客户端 → 服务端：请求打开工具外壳 GUI
 */
public record C2S_OpenToolShellPayload() implements CustomPacketPayload {
    public static final Type<C2S_OpenToolShellPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Relay.MOD_ID, "c2s_open_tool_shell"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_OpenToolShellPayload> CODEC =
        StreamCodec.unit(new C2S_OpenToolShellPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
