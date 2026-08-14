package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import qdream.relay.Relay;

/**
 * 客户端到服务端更新工具外壳配置的网络包
 * 用于同步 ToolShell 的配置项（使用背包能量模块、调试输出、统计信息）
 */
public record C2S_UpdateToolShellConfigPayload(
        boolean useInventoryEnergyModule,
        boolean debugOutputEnabled,
        boolean statusInfoEnabled
) implements CustomPacketPayload {

    public static final Type<C2S_UpdateToolShellConfigPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Relay.MOD_ID, "c2s_update_tool_shell_config")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_UpdateToolShellConfigPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    C2S_UpdateToolShellConfigPayload::useInventoryEnergyModule,
                    ByteBufCodecs.BOOL,
                    C2S_UpdateToolShellConfigPayload::debugOutputEnabled,
                    ByteBufCodecs.BOOL,
                    C2S_UpdateToolShellConfigPayload::statusInfoEnabled,
                    C2S_UpdateToolShellConfigPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
