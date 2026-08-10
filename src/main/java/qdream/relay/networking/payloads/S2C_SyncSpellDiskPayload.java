package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;

/**
 * 服务端 → 客户端：同步法术磁盘程序（JSON 字符串）
 */
public record S2C_SyncSpellDiskPayload(String programJson) implements CustomPacketPayload {
    public static final Type<S2C_SyncSpellDiskPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Relay.MOD_ID, "s2c_sync_spell_disk"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_SyncSpellDiskPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, S2C_SyncSpellDiskPayload::programJson,
            S2C_SyncSpellDiskPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
