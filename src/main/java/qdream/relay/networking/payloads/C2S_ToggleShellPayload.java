package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;

/**
 * 客户端 → 服务端：切换外壳开关状态
 */
public record C2S_ToggleShellPayload() implements CustomPacketPayload {
    public static final Type<C2S_ToggleShellPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Relay.MOD_ID, "c2s_toggle_shell"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_ToggleShellPayload> CODEC =
        StreamCodec.unit(new C2S_ToggleShellPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
