package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;

/**
 * 客户端 → 服务端：通知磁盘已放入编辑器插槽（服务端自己从插槽读取）
 */
public record C2S_DiskInsertedPayload() implements CustomPacketPayload {
    public static final Type<C2S_DiskInsertedPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Relay.MOD_ID, "c2s_disk_inserted"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_DiskInsertedPayload> CODEC = StreamCodec.unit(new C2S_DiskInsertedPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
