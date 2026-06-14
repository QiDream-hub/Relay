package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;

/**
 * 客户端 → 服务端：保存法术磁盘程序（将实体中的程序保存到磁盘）
 */
public record C2S_SaveSpellDiskPayload() implements CustomPacketPayload {
    public static final Type<C2S_SaveSpellDiskPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(Relay.MOD_ID, "c2s_save_spell_disk")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_SaveSpellDiskPayload> CODEC =
        StreamCodec.unit(new C2S_SaveSpellDiskPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
