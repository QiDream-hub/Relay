package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import qdream.relay.Relay;

/**
 * 客户端 → 服务端：工具外壳配置更新
 * 
 * <p>统一配置传递方案：单个网络包传递所有配置项</p>
 * 
 * <h3>配置项</h3>
 * <ul>
 * <li>useInventoryEnergyModule - 是否使用背包能量模块</li>
 * <li>debugOutputEnabled - 是否启用调试输出</li>
 * <li>statusInfoEnabled - 是否启用统计信息</li>
 * </ul>
 */
public record C2S_ToolShellConfigPayload(
        boolean useInventoryEnergyModule,
        boolean debugOutputEnabled,
        boolean statusInfoEnabled) implements CustomPacketPayload {
    
    public static final Type<C2S_ToolShellConfigPayload> TYPE = 
        new Type<>(Identifier.fromNamespaceAndPath(Relay.MOD_ID, "c2s_tool_shell_config"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_ToolShellConfigPayload> CODEC =
        StreamCodec.ofMember(
            (payload, buf) -> {
                buf.writeBoolean(payload.useInventoryEnergyModule);
                buf.writeBoolean(payload.debugOutputEnabled);
                buf.writeBoolean(payload.statusInfoEnabled);
            },
            buf -> new C2S_ToolShellConfigPayload(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
            )
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
