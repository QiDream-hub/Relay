package qdream.relay.networking.payloads;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import qdream.relay.Relay;

/**
 * 服务端 → 客户端：清理 Shell 日志缓存
 * 
 * <p>
 * 当方块被破坏时，服务端发送此网络包通知客户端清除对应坐标的日志缓存。
 * </p>
 */
public record S2C_ClearLogsPayload(
        BlockPos pos    // 方块坐标
) implements CustomPacketPayload {

    public static final Type<S2C_ClearLogsPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Relay.MOD_ID, "clear_logs"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_ClearLogsPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            S2C_ClearLogsPayload::pos,
            S2C_ClearLogsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
