package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import qdream.relay.Relay;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 → 客户端：同步 Shell 日志缓冲区
 */
public record S2C_ShellLogPayload(List<Component> logs) implements CustomPacketPayload {

    public static final Type<S2C_ShellLogPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Relay.MOD_ID, "shell_log"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_ShellLogPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ComponentSerialization.STREAM_CODEC),
            S2C_ShellLogPayload::logs,
            S2C_ShellLogPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
