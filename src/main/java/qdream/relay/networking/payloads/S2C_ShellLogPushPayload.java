package qdream.relay.networking.payloads;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import qdream.relay.Relay;

/**
 * 服务端 → 客户端：推送单条 Shell 日志
 * 
 * <p>
 * 与旧的 {@link S2C_ShellLogPayload} 不同，此网络包每次只推送单条日志，
 * 减少网络数据包大小，适合实时推送场景。
 * </p>
 */
public record S2C_ShellLogPushPayload(
        BlockPos pos,    // 方块坐标
        Component log    // 单条日志
) implements CustomPacketPayload {

    public static final Type<S2C_ShellLogPushPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Relay.MOD_ID, "shell_log_push"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_ShellLogPushPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            S2C_ShellLogPushPayload::pos,
            ComponentSerialization.STREAM_CODEC,
            S2C_ShellLogPushPayload::log,
            S2C_ShellLogPushPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
