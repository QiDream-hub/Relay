package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import qdream.relay.Relay;

/**
 * 客户端到服务端的外壳配置更新网络包
 * 用于同步 BlockShell 的调试输出和统计信息设置
 */
public record C2S_ShellConfigPayload(
        boolean debugOutputEnabled,
        boolean statusInfoEnabled
) implements CustomPacketPayload {

    public static final Type<C2S_ShellConfigPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Relay.MOD_ID, "c2s_shell_config")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_ShellConfigPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    C2S_ShellConfigPayload::debugOutputEnabled,
                    ByteBufCodecs.BOOL,
                    C2S_ShellConfigPayload::statusInfoEnabled,
                    C2S_ShellConfigPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
