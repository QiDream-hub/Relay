package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;

/**
 * 客户端 → 服务端：工具外壳配置更新
 */
public record C2S_ToolShellConfigPayload(boolean useInventoryEnergyModule) implements CustomPacketPayload {
    public static final Type<C2S_ToolShellConfigPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Relay.MOD_ID, "c2s_tool_shell_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_ToolShellConfigPayload> CODEC =
        StreamCodec.ofMember(
            (payload, buf) -> buf.writeBoolean(payload.useInventoryEnergyModule),
            buf -> new C2S_ToolShellConfigPayload(buf.readBoolean())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
