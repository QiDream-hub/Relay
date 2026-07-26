package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import qdream.relay.Relay;
import java.util.List;

/**
 * 服务端 → 客户端：同步 Shell 日志缓冲区
 */
public record S2C_ShellLogPayload(List<String> logs) implements CustomPacketPayload {

    public static final Type<S2C_ShellLogPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(Relay.MOD_ID, "shell_log")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_ShellLogPayload> CODEC =
        StreamCodec.composite(
            stringListCodec(),
            S2C_ShellLogPayload::logs,
            S2C_ShellLogPayload::new
        );
    
    /**
     * 字符串列表编解码器
     */
    private static StreamCodec<RegistryFriendlyByteBuf, List<String>> stringListCodec() {
        return StreamCodec.ofMember(
            (List<String> list, RegistryFriendlyByteBuf buf) -> {
                buf.writeInt(list.size());
                for (String s : list) {
                    buf.writeUtf(s);
                }
            },
            (RegistryFriendlyByteBuf buf) -> {
                int size = buf.readInt();
                java.util.List<String> list = new java.util.ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(buf.readUtf());
                }
                return list;
            }
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
