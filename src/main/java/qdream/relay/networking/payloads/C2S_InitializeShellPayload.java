package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;

/**
 * 客户端 → 服务端：初始化外壳（从磁盘加载程序）
 */
public record C2S_InitializeShellPayload() implements CustomPacketPayload {
    public static final Type<C2S_InitializeShellPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Relay.MOD_ID, "c2s_initialize_shell"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_InitializeShellPayload> CODEC =
        StreamCodec.unit(new C2S_InitializeShellPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
